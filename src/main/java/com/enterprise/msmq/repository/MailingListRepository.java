package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.MailingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for mailing lists.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Repository
public interface MailingListRepository extends JpaRepository<MailingList, Long> {
    
    /**
     * Find mailing list by list name.
     * 
     * @param listName the mailing list name
     * @return mailing list if found
     */
    Optional<MailingList> findByListName(String listName);
    
    /**
     * Find all active mailing lists.
     * 
     * @return list of active mailing lists
     */
    java.util.List<MailingList> findByIsActiveTrue();
    
    /**
     * Count active mailing lists.
     * 
     * @return count of active mailing lists
     */
    long countByIsActiveTrue();
    
    /**
     * Check if mailing list name exists.
     * 
     * @param listName the mailing list name
     * @return true if exists
     */
    boolean existsByListName(String listName);
    
    /**
     * Find mailing lists by email address.
     * 
     * @param emailAddress the email address to search for
     * @return list of mailing lists containing the email address
     */
    java.util.List<MailingList> findByEmailAddressesContaining(String emailAddress);
    
    /**
     * Find mailing lists by alert severity.
     * 
     * @param severity the alert severity
     * @return list of mailing lists configured for the severity
     */
    java.util.List<MailingList> findByAlertSeveritiesContaining(String severity);
    
    /**
     * Find mailing lists by alert type.
     * 
     * @param type the alert type
     * @return list of mailing lists configured for the type
     */
    java.util.List<MailingList> findByAlertTypesContaining(String type);
}
