package com.example.banking.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:banking-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BankingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAccountsDepositsTransfersAndReturnsPersistedBalances() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String sourceAccount = "source-" + suffix;
        String destinationAccount = "dest-" + suffix;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "%s",
                                  "customerName": "Source Customer",
                                  "currency": "USD"
                                }
                                """.formatted(sourceAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", is(sourceAccount)))
                .andExpect(jsonPath("$.balance", is("0.00")));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "%s",
                                  "customerName": "Destination Customer",
                                  "currency": "USD"
                                }
                                """.formatted(destinationAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", is(destinationAccount)));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "referenceId": "deposit-%s",
                                  "accountNumber": "%s",
                                  "amount": 100.00,
                                  "type": "DEPOSIT",
                                  "description": "Integration test deposit"
                                }
                                """.formatted(suffix, sourceAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("DEPOSIT")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "referenceId": "transfer-%s",
                                  "sourceAccountNumber": "%s",
                                  "destinationAccountNumber": "%s",
                                  "amount": 25.00
                                }
                                """.formatted(suffix, sourceAccount, destinationAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("TRANSFER")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(get("/api/accounts/{accountNumber}/balance", sourceAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is(sourceAccount)))
                .andExpect(jsonPath("$.balance", is("75.00")))
                .andExpect(jsonPath("$.currency", is("USD")));

        mockMvc.perform(get("/api/accounts/{accountNumber}/balance", destinationAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is(destinationAccount)))
                .andExpect(jsonPath("$.balance", is("25.00")))
                .andExpect(jsonPath("$.currency", is("USD")));
    }
}
