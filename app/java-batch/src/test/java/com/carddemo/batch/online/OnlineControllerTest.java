package com.carddemo.batch.online;

import com.carddemo.batch.testsupport.BatchFixtureFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The REST facade has to surface the CICS error messages instead of stack traces. */
@SpringBootTest
@AutoConfigureMockMvc
class OnlineControllerTest {

    private static final Path DIRECTORY = BatchFixtureFiles.createDirectory();

    @DynamicPropertySource
    static void files(DynamicPropertyRegistry registry) {
        registry.add("carddemo.card-xref-file", () -> DIRECTORY.resolve("cardxref.txt"));
        registry.add("carddemo.account-file", () -> DIRECTORY.resolve("acctdata.txt"));
        registry.add("carddemo.customer-file", () -> DIRECTORY.resolve("custdata.txt"));
        registry.add("carddemo.card-file", () -> DIRECTORY.resolve("carddata.txt"));
        registry.add("carddemo.transaction-file", () -> DIRECTORY.resolve("transact.txt"));
        registry.add("carddemo.user-security-file", () -> DIRECTORY.resolve("usrsec.txt"));
        registry.add("carddemo.user-credential-file", () -> DIRECTORY.resolve("usrsec.hash"));
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void writeFiles() {
        BatchFixtureFiles.writeOnlineData(DIRECTORY);
    }

    /** The bearer token that replaces the commarea the CICS sign on transaction filled in. */
    private String tokenFor(String userId, String password) throws Exception {
        String body = mockMvc.perform(post("/api/signon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new ObjectMapper().readTree(body).get("token").asText();
    }

    @Test
    void signsOnAndReturnsTheLandingProgram() throws Exception {
        mockMvc.perform(post("/api/signon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ADMIN001\",\"password\":\"PASSWORD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("A"))
                .andExpect(jsonPath("$.program").value("COADM01C"));
    }

    @Test
    void returnsTheScreenMessageAsABadRequestBody() throws Exception {
        mockMvc.perform(post("/api/signon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"ADMIN001\",\"password\":\"WRONG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Wrong Password. Try again ..."));
    }

    @Test
    void servesTheAccountViewAndTheCardList() throws Exception {
        String token = tokenFor("USER0001", "PASSWORD");

        mockMvc.perform(get("/api/accounts/11").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.accountId").value(11))
                .andExpect(jsonPath("$.cardNumber").value(BatchFixtureFiles.CARD_A));

        mockMvc.perform(get("/api/cards").param("accountId", "12").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardNumber").value(BatchFixtureFiles.CARD_C));
    }

    @Test
    void addsTheFirstTransactionWhenTheTransactionFileDoesNotExistYet() throws Exception {
        java.nio.file.Files.deleteIfExists(DIRECTORY.resolve("transact.txt"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenFor("USER0001", "PASSWORD"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"11","typeCode":"01","categoryCode":"1","source":"POS TERM",
                                 "description":"First","amount":"10.00",
                                 "originTimestamp":"2022-07-01 10:00:00.000000",
                                 "processTimestamp":"2022-07-01 10:00:00.000000","merchantId":"800000000",
                                 "merchantName":"Abshire-Lowe","merchantCity":"Seattle","merchantZip":"99999"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("0000000000000001"));
    }

    @Test
    void neverReturnsThePasswordInTheUserRepresentation() throws Exception {
        mockMvc.perform(get("/api/users/ADMIN001").header("Authorization", tokenFor("ADMIN001", "PASSWORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ADMIN001"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void listsTheMenuOfTheSignedOnUserRatherThanARequestedOne() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", tokenFor("USER0001", "PASSWORD"))
                        .param("userType", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].program").value("COACTVWC"));

        mockMvc.perform(get("/api/menu").header("Authorization", tokenFor("ADMIN001", "PASSWORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].program").value("COUSR00C"));
    }

    @Test
    void refusesAnonymousAccessToEverythingButSignOn() throws Exception {
        mockMvc.perform(get("/api/accounts/11")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/menu")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/USER0001")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/menu").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void keepsTheUserAdministrationScreensForAdministrators() throws Exception {
        String user = tokenFor("USER0001", "PASSWORD");

        mockMvc.perform(get("/api/users").header("Authorization", user))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/ADMIN001").header("Authorization", user))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", tokenFor("ADMIN001", "PASSWORD")))
                .andExpect(status().isOk());
    }
}
