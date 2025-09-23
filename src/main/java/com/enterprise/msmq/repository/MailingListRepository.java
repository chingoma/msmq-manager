package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.MailingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for MailingList entities.
 * Provides database access methods for mailing list management.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface MailingListRepository extends JpaRepository<MailingList, Long> {

    /**
     * Finds a mailing list by its name.
     *
     * @param listName the mailing list name
     * @return Optional containing the mailing list if found
     */
    Optional<MailingList> findByListName(String listName);

    /**
     * Finds all active mailing lists.
     *
     * @return List of active mailing lists
     */
    @Query("SELECT m FROM MailingList m WHERE m.isActive = true")
    java.util.List<MailingList> findAllActive();

    /**
     * Finds all active mailing lists.
     *
     * @return List of active mailing lists
     */
    java.util.List<MailingList> findByIsActiveTrue();

    /**
     * Checks if a mailing list with the given name exists.
     *
     * @param listName the mailing list name
     * @return true if mailing list exists, false otherwise
     */
    boolean existsByListName(String listName);

    /**
     * Counts active mailing lists.
     *
     * @return count of active mailing lists
     */
    long countByIsActiveTrue();
}
