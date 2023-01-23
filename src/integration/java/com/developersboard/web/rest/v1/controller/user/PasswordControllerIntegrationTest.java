package com.developersboard.web.rest.v1.controller.user;

import com.developersboard.IntegrationTestUtils;
import com.developersboard.constant.EmailConstants;
import com.developersboard.constant.ErrorConstants;
import com.developersboard.constant.user.PasswordConstants;
import com.developersboard.shared.util.UserUtils;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Isolated
@Disabled
class PasswordControllerIntegrationTest extends IntegrationTestUtils {

  @Test
  void startingForgotPasswordWithUserNotExisting(TestInfo testInfo) throws Exception {

    this.mockMvc
        .perform(
            MockMvcRequestBuilders.post(PasswordConstants.PASSWORD_RESET_ROOT_MAPPING)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("email", testInfo.getDisplayName()))
        .andExpect(
            MockMvcResultMatchers.model()
                .attributeExists(PasswordConstants.PASSWORD_RESET_EMAIL_SENT_KEY))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void startingForgotPasswordShouldPresentSendPasswordResetEmail() throws Exception {

    var userDto = UserUtils.createUserDto(true);
    var storedUser = createAndAssertUser(userDto);

    greenMail.start();

    this.mockMvc
        .perform(
            MockMvcRequestBuilders.post(PasswordConstants.PASSWORD_RESET_ROOT_MAPPING)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("email", storedUser.getEmail()))
        .andExpect(
            MockMvcResultMatchers.model()
                .attributeExists(PasswordConstants.PASSWORD_RESET_EMAIL_SENT_KEY))
        .andExpect(MockMvcResultMatchers.model().attributeDoesNotExist(ErrorConstants.ERROR))
        .andExpect(
            MockMvcResultMatchers.view().name(PasswordConstants.PASSWORD_RESET_START_VIEW_NAME))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // Retrieve using GreenMail API
    Message[] messages = greenMail.getReceivedMessages();
    String passwordResetEmailBody = messages[0].getContent().toString();

    Assertions.assertTrue(
        passwordResetEmailBody.contains("http://localhost/password-reset/change?token="));
    Assertions.assertEquals(EmailConstants.PASSWORD_RESET_EMAIL_SUBJECT, messages[0].getSubject());
    Assertions.assertTrue(
        messages[0].getRecipients(RecipientType.TO)[0].toString().contains(storedUser.getEmail()));
    Assertions.assertEquals(1, messages.length);

    greenMail.stop();
  }
}
