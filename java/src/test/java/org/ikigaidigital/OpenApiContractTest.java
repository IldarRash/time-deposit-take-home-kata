package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {
    @Test
    void contractDescribesBothOperationsAndTheirResponseSchemas() throws Exception {
        Map<String, Object> document;
        try (var source = Files.newInputStream(Path.of("openapi.yaml"))) {
            document = map(new Yaml().load(source));
        }

        assertThat(document.get("openapi")).isEqualTo("3.0.3");
        Map<String, Object> paths = map(document.get("paths"));
        assertThat(paths.keySet()).containsExactlyInAnyOrder(
                "/time-deposits", "/time-deposits/update-balances");

        Map<String, Object> listOperation = map(paths.get("/time-deposits"));
        assertThat(listOperation.keySet()).containsExactly("get");
        Map<String, Object> listResponse = map(map(listOperation.get("get")).get("responses"));
        assertThat(listResponse.keySet()).containsExactly("200");
        Map<String, Object> listContent = map(map(listResponse.get("200")).get("content"));
        assertThat(listContent.keySet()).containsExactly("application/json");
        Map<String, Object> listSchema = map(map(listContent.get("application/json")).get("schema"));
        assertThat(listSchema.get("type")).isEqualTo("array");
        assertThat(map(listSchema.get("items"))).containsExactly(
                Map.entry("$ref", "#/components/schemas/TimeDeposit"));

        Map<String, Object> updateOperation = map(paths.get("/time-deposits/update-balances"));
        assertThat(updateOperation.keySet()).containsExactly("post");
        Map<String, Object> updateResponse = map(map(updateOperation.get("post")).get("responses"));
        assertThat(updateResponse.keySet()).containsExactly("204");
        assertThat(map(updateResponse.get("204"))).doesNotContainKey("content");

        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        assertThat(schemas.keySet()).containsExactlyInAnyOrder("TimeDeposit", "Withdrawal");
        assertTimeDepositSchema(map(schemas.get("TimeDeposit")));
        assertWithdrawalSchema(map(schemas.get("Withdrawal")));
    }

    private void assertTimeDepositSchema(Map<String, Object> schema) {
        assertObjectSchema(schema, Set.of("id", "planType", "balance", "days", "withdrawals"));
        Map<String, Object> properties = map(schema.get("properties"));
        assertThat(properties.keySet()).containsExactlyInAnyOrder("id", "planType", "balance", "days", "withdrawals");
        assertIntegerProperty(properties.get("id"));
        assertThat(map(properties.get("planType")).get("type")).isEqualTo("string");
        assertThat(map(properties.get("balance")).get("type")).isEqualTo("number");
        assertIntegerProperty(properties.get("days"));
        Map<String, Object> withdrawals = map(properties.get("withdrawals"));
        assertThat(withdrawals.get("type")).isEqualTo("array");
        assertThat(map(withdrawals.get("items"))).containsExactly(
                Map.entry("$ref", "#/components/schemas/Withdrawal"));
    }

    private void assertWithdrawalSchema(Map<String, Object> schema) {
        assertObjectSchema(schema, Set.of("id", "amount", "date"));
        Map<String, Object> properties = map(schema.get("properties"));
        assertThat(properties.keySet()).containsExactlyInAnyOrder("id", "amount", "date");
        assertIntegerProperty(properties.get("id"));
        assertThat(map(properties.get("amount")).get("type")).isEqualTo("number");
        Map<String, Object> date = map(properties.get("date"));
        assertThat(date.get("type")).isEqualTo("string");
        assertThat(date.get("format")).isEqualTo("date");
    }

    private void assertObjectSchema(Map<String, Object> schema, Set<String> requiredProperties) {
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(Set.copyOf(strings(schema.get("required")))).isEqualTo(requiredProperties);
    }

    private void assertIntegerProperty(Object property) {
        Map<String, Object> value = map(property);
        assertThat(value.get("type")).isEqualTo("integer");
        assertThat(value.get("format")).isEqualTo("int32");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}
