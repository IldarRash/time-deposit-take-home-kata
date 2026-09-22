package org.ikigaidigital;

import org.ikigaidigital.adapter.in.web.DepositController;
import org.ikigaidigital.application.DepositService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpenApiContractTest {
    @Test
    @SuppressWarnings("unchecked")
    void contractOperationsMatchTheTwoRuntimeControllerMappings() throws Exception {
        Map<String, Object> document;
        try (var source = Files.newInputStream(Path.of("openapi.yaml"))) {
            document = new Yaml().load(source);
        }
        assertThat(document.get("openapi")).isEqualTo("3.0.3");
        var paths = (Map<String, Map<String, Object>>) document.get("paths");
        Set<String> contractOperations = new HashSet<>();
        paths.forEach((path, methods) -> methods.keySet().forEach(method ->
                contractOperations.add(method.toUpperCase() + " " + path)));
        assertThat(contractOperations).containsExactlyInAnyOrder(
                "GET /time-deposits", "POST /time-deposits/update-balances");

        try (var context = new StaticWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getBeanFactory().registerSingleton("controller", new DepositController(mock(DepositService.class)));
            context.refresh();
            var mappings = new RequestMappingHandlerMapping();
            mappings.setApplicationContext(context);
            mappings.afterPropertiesSet();
            Set<String> runtimeOperations = new HashSet<>();
            mappings.getHandlerMethods().keySet().forEach(mapping -> {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    for (String path : mapping.getPatternValues()) {
                        runtimeOperations.add(method + " " + path);
                    }
                }
            });
            assertThat(runtimeOperations).isEqualTo(contractOperations);
        }
    }
}
