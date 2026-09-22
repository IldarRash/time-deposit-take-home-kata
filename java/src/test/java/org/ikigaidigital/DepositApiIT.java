package org.ikigaidigital;

import com.fasterxml.jackson.databind.JsonNode;
import org.ikigaidigital.adapter.in.web.DepositController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DepositApiIT extends PostgresIntegrationSupport {
    private static final String FRAMEWORK_ERROR_CONTROLLER =
            "org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController";

    @Autowired TestRestTemplate http;
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void fullApplicationContextExposesOnlyTheTwoDepositControllerOperations() {
        Map<RequestMappingInfo, HandlerMethod> mappings = requestMappingHandlerMapping.getHandlerMethods();
        assertThat(mappings.values())
                .allSatisfy(handler -> assertThat(handler.getBeanType().getName())
                        .isIn(DepositController.class.getName(), FRAMEWORK_ERROR_CONTROLLER));

        Set<String> applicationOperations = new HashSet<>();
        mappings.forEach((mapping, handler) -> {
            if (handler.getBeanType().equals(DepositController.class)) {
                assertThat(mapping.getMethodsCondition().getMethods()).isNotEmpty();
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    for (String path : mapping.getPatternValues()) {
                        applicationOperations.add(method + " " + path);
                    }
                }
            }
        });

        assertThat(applicationOperations).containsExactlyInAnyOrder(
                "GET /time-deposits", "POST /time-deposits/update-balances");
    }

    @Test
    void anEmptyDatabaseReturnsAnArrayAndAcceptsAnUpdate() {
        var get = http.getForEntity("/time-deposits", JsonNode.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(get.getBody().isArray()).isTrue();
        assertThat(get.getBody().size()).isZero();

        var post = http.postForEntity("/time-deposits/update-balances", null, String.class);
        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(post.getHeaders().getContentType()).isNull();
        assertThat(post.getBody()).isNull();
    }

    @Test
    void getReturnsTheRequiredSchemaAndNestedWithdrawalDates() {
        insertDeposit(2, "student", 365, "1200.00");
        insertDeposit(1, "basic", 31, "1200.00");
        jdbc.update("INSERT INTO withdrawals VALUES (10, 1, 25.50, '2026-01-15')");

        var response = http.getForEntity("/time-deposits", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        JsonNode deposits = response.getBody();
        assertThat(deposits.isArray()).isTrue();
        assertThat(deposits.size()).isEqualTo(2);
        JsonNode first = deposits.get(0);
        var fields = new ArrayList<String>();
        first.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("id", "planType", "balance", "days", "withdrawals");
        assertThat(first.get("id").isInt()).isTrue();
        assertThat(first.get("id").asInt()).isEqualTo(1);
        assertThat(first.get("planType").isTextual()).isTrue();
        assertThat(first.get("planType").asText()).isEqualTo("basic");
        assertThat(first.get("balance").isNumber()).isTrue();
        assertThat(first.get("balance").asDouble()).isEqualTo(1200.0);
        assertThat(first.get("days").isInt()).isTrue();
        assertThat(first.get("days").asInt()).isEqualTo(31);
        assertThat(first.get("withdrawals").isArray()).isTrue();
        JsonNode withdrawal = first.get("withdrawals").get(0);
        var withdrawalFields = new ArrayList<String>();
        withdrawal.fieldNames().forEachRemaining(withdrawalFields::add);
        assertThat(withdrawalFields).containsExactlyInAnyOrder("id", "amount", "date");
        assertThat(withdrawal.get("id").isInt()).isTrue();
        assertThat(withdrawal.get("id").asInt()).isEqualTo(10);
        assertThat(withdrawal.get("amount").isNumber()).isTrue();
        assertThat(withdrawal.get("amount").asDouble()).isEqualTo(25.5);
        assertThat(withdrawal.get("date").isTextual()).isTrue();
        assertThat(withdrawal.get("date").asText()).isEqualTo("2026-01-15");
        assertThat(deposits.get(1).get("withdrawals").isArray()).isTrue();
        assertThat(deposits.get(1).get("withdrawals").size()).isZero();
    }

    @Test
    void postPersistsAllBalancesAndASecondPostAccruesAgain() {
        insertDeposit(1, "basic", 31, "1200");
        insertDeposit(2, "premium", 46, "1200");
        insertDeposit(3, "unknown", 46, "1200");
        jdbc.update("INSERT INTO withdrawals VALUES (10, 1, 25.00, '2026-01-15')");

        var firstPost = http.postForEntity("/time-deposits/update-balances", null, String.class);

        assertThat(firstPost.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(firstPost.getBody()).isNull();
        assertThat(balance(1)).isEqualByComparingTo("1201");
        assertThat(balance(2)).isEqualByComparingTo("1205");
        assertThat(balance(3)).isEqualByComparingTo("1200");
        JsonNode firstGet = http.getForObject("/time-deposits", JsonNode.class);
        assertThat(firstGet.get(0).get("balance").asDouble()).isEqualTo(1201.0);
        assertThat(firstGet.get(1).get("balance").asDouble()).isEqualTo(1205.0);

        var secondPost = http.postForEntity("/time-deposits/update-balances", null, String.class);

        assertThat(secondPost.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode secondGet = http.getForObject("/time-deposits", JsonNode.class);
        assertThat(secondGet.get(0).get("balance").asDouble()).isEqualTo(1202.0);
        assertThat(secondGet.get(1).get("balance").asDouble()).isEqualTo(1210.02);
        assertThat(secondGet.get(2).get("balance").asDouble()).isEqualTo(1200.0);
        assertThat(secondGet.get(0).get("days").asInt()).isEqualTo(31);
        assertThat(secondGet.get(0).get("withdrawals")).isEqualTo(firstGet.get(0).get("withdrawals"));
    }

    @Test
    void noAdditionalBusinessOrDocumentationRoutesAreExposed() {
        assertThat(http.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/v3/api-docs", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.postForEntity("/time-deposits", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
