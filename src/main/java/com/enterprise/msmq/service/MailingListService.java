package com.enterprise.msmq.service;

import com.enterprise.msmq.entity.MailingList;
import com.enterprise.msmq.repository.MailingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing mailing lists.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MailingListService {
    
    private final MailingListRepository mailingListRepository;
    
    /**
     * Save or update mailing list.
     * 
     * @param mailingList the mailing list to save
     * @return saved mailing list
     */
    public MailingList saveMailingList(MailingList mailingList) {
        MailingList saved = mailingListRepository.save(mailingList);
        log.info("Saved mailing list: {}", saved.getListName());
        return saved;
    }
    
    /**
     * Get mailing list by ID.
     * 
     * @param id the mailing list ID
     * @return mailing list if found
     */
    public Optional<MailingList> getMailingListById(Long id) {
        return mailingListRepository.findById(id);
    }
    
    /**
     * Get mailing list by name.
     * 
     * @param listName the mailing list name
     * @return mailing list if found
     */
    public Optional<MailingList> getMailingListByName(String listName) {
        return mailingListRepository.findByListName(listName);
    }
    
    /**
     * Get all active mailing lists.
     * 
     * @return list of active mailing lists
     */
    public List<MailingList> findActiveMailingLists() {
        return mailingListRepository.findByIsActiveTrue();
    }
    
    /**
     * Get all mailing lists.
     * 
     * @return list of all mailing lists
     */
    public List<MailingList> getAllMailingLists() {
        return mailingListRepository.findAll();
    }
    
    /**
     * Delete mailing list.
     * 
     * @param id the mailing list ID
     */
    public void deleteMailingList(Long id) {
        Optional<MailingList> mailingList = mailingListRepository.findById(id);
        if (mailingList.isPresent()) {
            mailingListRepository.deleteById(id);
            log.info("Deleted mailing list: {}", mailingList.get().getListName());
        }
    }
    
    /**
     * Activate mailing list.
     * 
     * @param id the mailing list ID
     */
    public void activateMailingList(Long id) {
        mailingListRepository.findById(id).ifPresent(mailingList -> {
            mailingList.setIsActive(true);
            mailingListRepository.save(mailingList);
            log.info("Activated mailing list: {}", mailingList.getListName());
        });
    }
    
    /**
     * Deactivate mailing list.
     * 
     * @param id the mailing list ID
     */
    public void deactivateMailingList(Long id) {
        mailingListRepository.findById(id).ifPresent(mailingList -> {
            mailingList.setIsActive(false);
            mailingListRepository.save(mailingList);
            log.info("Deactivated mailing list: {}", mailingList.getListName());
        });
    }
    
    /**
     * Add email address to mailing list.
     * 
     * @param id the mailing list ID
     * @param emailAddress the email address to add
     */
    public void addEmailToMailingList(Long id, String emailAddress) {
        mailingListRepository.findById(id).ifPresent(mailingList -> {
            if (mailingList.getEmailAddresses() != null && 
                !mailingList.getEmailAddresses().contains(emailAddress)) {
                mailingList.getEmailAddresses().add(emailAddress);
                mailingListRepository.save(mailingList);
                log.info("Added email {} to mailing list: {}", emailAddress, mailingList.getListName());
            }
        });
    }
    
    /**
     * Remove email address from mailing list.
     * 
     * @param id the mailing list ID
     * @param emailAddress the email address to remove
     */
    public void removeEmailFromMailingList(Long id, String emailAddress) {
        mailingListRepository.findById(id).ifPresent(mailingList -> {
            if (mailingList.getEmailAddresses() != null) {
                mailingList.getEmailAddresses().remove(emailAddress);
                mailingListRepository.save(mailingList);
                log.info("Removed email {} from mailing list: {}", emailAddress, mailingList.getListName());
            }
        });
    }
    
    /**
     * Count active mailing lists.
     * 
     * @return count of active mailing lists
     */
    public long countActiveMailingLists() {
        return mailingListRepository.countByIsActiveTrue();
    }
    
    /**
     * Validate mailing list.
     * 
     * @param mailingList the mailing list to validate
     * @return validation result
     */
    public boolean validateMailingList(MailingList mailingList) {
        if (mailingList.getListName() == null || mailingList.getListName().trim().isEmpty()) {
            return false;
        }
        
        if (mailingList.getEmailAddresses() == null || mailingList.getEmailAddresses().isEmpty()) {
            return false;
        }
        
        // Validate email addresses format (basic validation)
        for (String email : mailingList.getEmailAddresses()) {
            if (!isValidEmail(email)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if email address is valid.
     * 
     * @param email the email address to validate
     * @return true if valid
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Get mailing lists that should receive alerts for specific severity and type.
     * 
     * @param severity the alert severity
     * @param type the alert type
     * @return list of relevant mailing lists
     */
    public List<MailingList> getMailingListsForAlert(com.enterprise.msmq.enums.AlertSeverity severity, 
                                                     com.enterprise.msmq.enums.AlertType type) {
        List<MailingList> activeLists = findActiveMailingLists();
        return activeLists.stream()
            .filter(list -> list.shouldReceiveAlert(severity, type))
            .toList();
    }
}
