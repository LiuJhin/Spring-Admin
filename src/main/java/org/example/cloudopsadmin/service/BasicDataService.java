package org.example.cloudopsadmin.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.entity.CloudProvider;
import org.example.cloudopsadmin.entity.CreditCard;
import org.example.cloudopsadmin.entity.PartnerBd;
import org.example.cloudopsadmin.repository.AccountRepository;
import org.example.cloudopsadmin.repository.CloudProviderRepository;
import org.example.cloudopsadmin.repository.CreditCardRepository;
import org.example.cloudopsadmin.repository.PartnerBdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BasicDataService {

    private final CloudProviderRepository cloudProviderRepository;
    private final PartnerBdRepository partnerBdRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountRepository accountRepository;

    // Cloud Provider Methods
    public List<CloudProvider> getAllCloudProviders() {
        return cloudProviderRepository.findAll();
    }

    @Transactional
    public CloudProvider createCloudProvider(CloudProvider provider) {
        return cloudProviderRepository.save(provider);
    }

    @Transactional
    public CloudProvider updateCloudProvider(Long id, CloudProvider providerDetails) {
        CloudProvider provider = cloudProviderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cloud Provider not found"));
        
        provider.setName(providerDetails.getName());
        provider.setCode(providerDetails.getCode());
        provider.setStatus(providerDetails.getStatus());
        provider.setDescription(providerDetails.getDescription());
        
        return cloudProviderRepository.save(provider);
    }

    @Transactional
    public void deleteCloudProvider(Long id) {
        cloudProviderRepository.deleteById(id);
    }

    // Partner BD Methods
    public List<PartnerBd> getAllPartnerBds() {
        return partnerBdRepository.findAll();
    }

    @Transactional
    public PartnerBd createPartnerBd(PartnerBd bd) {
        return partnerBdRepository.save(bd);
    }

    @Transactional
    public PartnerBd updatePartnerBd(Long id, PartnerBd bdDetails) {
        PartnerBd bd = partnerBdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner BD not found"));

        bd.setName(bdDetails.getName());
        bd.setEmail(bdDetails.getEmail());
        bd.setPhone(bdDetails.getPhone());
        bd.setStatus(bdDetails.getStatus());
        bd.setDescription(bdDetails.getDescription());

        return partnerBdRepository.save(bd);
    }

    @Transactional
    public void deletePartnerBd(Long id) {
        partnerBdRepository.deleteById(id);
    }

    // Credit Card Methods
    public List<CreditCard> getAllCreditCards() {
        List<CreditCard> cards = creditCardRepository.findAll();
        // Calculate linked account count
        for (CreditCard card : cards) {
            String masked = constructMaskedCard(card.getFirstFourDigits(), card.getLastFourDigits());
            int count = accountRepository.countByBoundCreditCardMasked(masked);
            card.setLinkedAccountCount(count);
        }
        return cards;
    }

    @Transactional
    public CreditCard createCreditCard(CreditCard card) {
        // Here we should ideally encrypt the card number, but for now we store as is or assume handled
        // Also validate format if needed
        return creditCardRepository.save(card);
    }

    @Transactional
    public CreditCard updateCreditCard(Long id, CreditCard cardDetails) {
        CreditCard card = creditCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit Card not found"));

        card.setBankName(cardDetails.getBankName());
        card.setHolderName(cardDetails.getHolderName());
        card.setFirstFourDigits(cardDetails.getFirstFourDigits());
        card.setLastFourDigits(cardDetails.getLastFourDigits());
        card.setExpirationDate(cardDetails.getExpirationDate());
        card.setStatus(cardDetails.getStatus());
        card.setDescription(cardDetails.getDescription());

        return creditCardRepository.save(card);
    }

    @Transactional
    public void deleteCreditCard(Long id) {
        creditCardRepository.deleteById(id);
    }

    private String constructMaskedCard(String first4, String last4) {
        if (!StringUtils.hasText(last4)) return "************";
        // Assuming the mask format in Account is "************" + last4
        // Or if Account stores full masked version like "1234********5678", we need to know.
        // Based on AccountService read earlier: 
        // return "************" + last4;
        // So we just use last4.
        
        // Wait, AccountService.maskCreditCard logic was:
        // if (digits.length() < 4) return "************";
        // String last4 = digits.substring(digits.length() - 4);
        // return "************" + last4;
        
        // So it ignores first digits in the mask.
        return "************" + last4;
    }
}
