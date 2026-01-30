package org.example.cloudopsadmin;

import org.example.cloudopsadmin.entity.Account;
import org.example.cloudopsadmin.entity.Email;
import org.example.cloudopsadmin.entity.Payer;
import org.example.cloudopsadmin.repository.AccountRepository;
import org.example.cloudopsadmin.repository.EmailRepository;
import org.example.cloudopsadmin.repository.PayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cloudopsadmin.service.AccountService;
import org.example.cloudopsadmin.service.AccountService.AddAccountRequest;
import org.example.cloudopsadmin.service.AccountService.ApiException;
import org.example.cloudopsadmin.service.AccountService.UpdateAccountRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class EmailOneToOneAccountTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private PayerRepository payerRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    private Email sharedEmail;
    private Payer testPayer;

    @BeforeEach
    public void setup() {
        // Create a Payer
        testPayer = new Payer();
        testPayer.setPayerId("123456789012"); // 12 digits
        testPayer.setPayerInternalId("payer_internal_123");
        testPayer.setPayerName("Test Payer");
        testPayer.setSigninUrl("https://console.aws.amazon.com");
        testPayer.setIamUsername("admin");
        testPayer.setPassword("password");
        testPayer.setContactEmail("payer@example.com");
        testPayer = payerRepository.save(testPayer);

        // Create an Email
        sharedEmail = new Email();
        sharedEmail.setEmailAddress("shared@example.com");
        sharedEmail.setEmailInternalId("email_shared_001");
        sharedEmail.setPassword("password");
        sharedEmail.setSource("test");
        sharedEmail.setCategory("normal");
        sharedEmail.setStatus("active");
        sharedEmail = emailRepository.save(sharedEmail);
    }

    @Test
    @WithMockUser(authorities = "ACCOUNT_MANAGE")
    public void testAddAccountWithBoundEmail() {
        // 1. Create first account bound to sharedEmail
        Account acc1 = new Account();
        acc1.setUid("111111111111");
        acc1.setAccountInternalId("acc_test_001");
        acc1.setAccountName("Account 1");
        acc1.setAccountType("AWS");
        acc1.setAccountCategory("normal");
        acc1.setAccountSource("API");
        acc1.setAccountAttribution("Test");
        acc1.setBoundCreditCardEncrypted("enc");
        acc1.setBoundCreditCardMasked("****1234");
        acc1.setBoundEmail("test@example.com");
        acc1.setMonitorEmail("monitor@example.com");
        acc1.setMonitorUrl("http://example.com");
        acc1.setMonitorBillGroup("false");
        acc1.setIsMonitoredSp(false);
        acc1.setIsSubmitted(false);
        acc1.setSendPo(false);
        acc1.setRiskDiscount(0.0);
        acc1.setCostDiscount(0.0);
        acc1.setLinkedEmail(sharedEmail);
        accountRepository.save(acc1);

        // 2. Try to create second account with same email
        Map<String, Object> map = new HashMap<>();
        map.put("uid", "222222222222");
        map.put("account_name", "Account 2");
        map.put("vendor", "AWS");
        map.put("account_type", "normal"); // maps to accountCategory
        map.put("account_source", "API");
        map.put("account_attribution", "Test");
        map.put("is_monitored_sp", false);
        map.put("monitor_bill_group", "false");
        map.put("email_address", "shared@example.com"); // Same email address
        map.put("payer_id", "payer_internal_123");
        map.put("monitor_email", "monitor@example.com");
        map.put("bound_email", "bound@example.com");
        map.put("bound_credit_card", "****1234");
        map.put("risk_discount", 0.0);
        map.put("cost_discount", 0.0);
        
        AddAccountRequest request = objectMapper.convertValue(map, AddAccountRequest.class);

        ApiException exception = assertThrows(ApiException.class, () -> {
            accountService.addAccount(request, null);
        });
        
        Assertions.assertEquals("该邮箱已被其他账号绑定", exception.getMessage());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNT_MANAGE")
    public void testUpdateAccountToBoundEmail() {
        // 1. Create first account bound to sharedEmail
        Account acc1 = new Account();
        acc1.setUid("111111111111");
        acc1.setAccountInternalId("acc_test_001");
        acc1.setAccountName("Account 1");
        acc1.setAccountType("AWS");
        acc1.setAccountCategory("normal");
        acc1.setAccountSource("API");
        acc1.setAccountAttribution("Test");
        acc1.setBoundCreditCardEncrypted("enc");
        acc1.setBoundCreditCardMasked("****1234");
        acc1.setBoundEmail("test@example.com");
        acc1.setMonitorEmail("monitor@example.com");
        acc1.setMonitorUrl("http://example.com");
        acc1.setMonitorBillGroup("false");
        acc1.setIsMonitoredSp(false);
        acc1.setIsSubmitted(false);
        acc1.setSendPo(false);
        acc1.setRiskDiscount(0.0);
        acc1.setCostDiscount(0.0);
        acc1.setLinkedEmail(sharedEmail);
        accountRepository.save(acc1);

        // 2. Create second account bound to OTHER email
        Email otherEmail = new Email();
        otherEmail.setEmailAddress("other@example.com");
        otherEmail.setEmailInternalId("email_other_001");
        otherEmail.setPassword("password");
        otherEmail.setSource("test");
        otherEmail.setCategory("normal");
        otherEmail.setStatus("active");
        otherEmail = emailRepository.save(otherEmail);

        Account acc2 = new Account();
        acc2.setUid("222222222222");
        acc2.setAccountInternalId("acc_test_002");
        acc2.setAccountName("Account 2");
        acc2.setAccountType("AWS");
        acc2.setAccountCategory("normal");
        acc2.setAccountSource("API");
        acc2.setAccountAttribution("Test");
        acc2.setBoundCreditCardEncrypted("enc");
        acc2.setBoundCreditCardMasked("****1234");
        acc2.setBoundEmail("test@example.com");
        acc2.setMonitorEmail("monitor@example.com");
        acc2.setMonitorUrl("http://example.com");
        acc2.setMonitorBillGroup("false");
        acc2.setIsMonitoredSp(false);
        acc2.setIsSubmitted(false);
        acc2.setSendPo(false);
        acc2.setRiskDiscount(0.0);
        acc2.setCostDiscount(0.0);
        acc2.setLinkedEmail(otherEmail);
        accountRepository.save(acc2);

        // 3. Try to update Account 2 to use sharedEmail (which is bound to Account 1)
        Map<String, Object> map = new HashMap<>();
        map.put("uid", "222222222222");
        map.put("email_id", "email_shared_001"); // Trying to switch to shared email
        
        UpdateAccountRequest request = objectMapper.convertValue(map, UpdateAccountRequest.class);

        ApiException exception = assertThrows(ApiException.class, () -> {
            accountService.updateAccount(request, null);
        });

        Assertions.assertEquals("该邮箱已被其他账号绑定", exception.getMessage());
    }

    @Test
    @WithMockUser(authorities = "ACCOUNT_MANAGE")
    public void testUpdateAccountSelfBinding() {
        // 1. Create account bound to sharedEmail
        Account acc1 = new Account();
        acc1.setUid("111111111111");
        acc1.setAccountInternalId("acc_test_001");
        acc1.setAccountName("Account 1");
        acc1.setAccountType("AWS");
        acc1.setAccountCategory("normal");
        acc1.setAccountSource("API");
        acc1.setAccountAttribution("Test");
        acc1.setBoundCreditCardEncrypted("enc");
        acc1.setBoundCreditCardMasked("****1234");
        acc1.setBoundEmail("test@example.com");
        acc1.setMonitorEmail("monitor@example.com");
        acc1.setMonitorUrl("http://example.com");
        acc1.setMonitorBillGroup("false");
        acc1.setIsMonitoredSp(false);
        acc1.setIsSubmitted(false);
        acc1.setSendPo(false);
        acc1.setRiskDiscount(0.0);
        acc1.setCostDiscount(0.0);
        acc1.setLinkedEmail(sharedEmail);
        acc1.setPayer(testPayer);
        accountRepository.save(acc1);

        // 2. Update same account with same email (should pass)
        Map<String, Object> map = new HashMap<>();
        map.put("uid", "111111111111");
        map.put("email_id", "email_shared_001");
        
        UpdateAccountRequest request = objectMapper.convertValue(map, UpdateAccountRequest.class);

        // Should not throw exception
        accountService.updateAccount(request, null);
    }
}
