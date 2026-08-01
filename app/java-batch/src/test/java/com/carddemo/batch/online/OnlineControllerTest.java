package com.carddemo.batch.online;

import com.carddemo.batch.testsupport.BatchFixtureFiles;
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
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void writeFiles() {
        BatchFixtureFiles.writeOnlineData(DIRECTORY);
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
        mockMvc.perform(get("/api/accounts/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.accountId").value(11))
                .andExpect(jsonPath("$.cardNumber").value(BatchFixtureFiles.CARD_A));

        mockMvc.perform(get("/api/cards").param("accountId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardNumber").value(BatchFixtureFiles.CARD_C));
    }

    @Test
    void listsTheMainMenuOptions() throws Exception {
        mockMvc.perform(get("/api/menu").param("userType", "U"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].program").value("COACTVWC"));
    }
}
