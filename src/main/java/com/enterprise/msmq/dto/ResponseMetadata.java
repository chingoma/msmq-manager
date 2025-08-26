package com.enterprise.msmq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Metadata information for API responses.
 * 
 * This class provides additional context about the response including
 * processing time, pagination details, and other metadata.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMetadata {

    /**
     * Request processing time in milliseconds.
     */
    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    /**
     * Total number of records available.
     */
    @JsonProperty("totalRecords")
    private Long totalRecords;

    /**
     * Current page number for paginated responses.
     */
    @JsonProperty("currentPage")
    private Integer currentPage;

    /**
     * Page size for paginated responses.
     */
    @JsonProperty("pageSize")
    private Integer pageSize;

    /**
     * Total number of pages for paginated responses.
     */
    @JsonProperty("totalPages")
    private Integer totalPages;

    /**
     * Server timestamp when the response was generated.
     */
    @JsonProperty("serverTimestamp")
    private LocalDateTime serverTimestamp;

    /**
     * API version information.
     */
    @JsonProperty("apiVersion")
    private String apiVersion;

    /**
     * Additional custom metadata.
     */
    @JsonProperty("customData")
    private Object customData;

    /**
     * Default constructor.
     */
    public ResponseMetadata() {
        this.serverTimestamp = LocalDateTime.now();
        this.apiVersion = "1.0.0";
    }

    /**
     * Constructor with processing time.
     * 
     * @param processingTimeMs the processing time in milliseconds
     */
    public ResponseMetadata(Long processingTimeMs) {
        this();
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Constructor with pagination details.
     * 
     * @param totalRecords the total number of records
     * @param currentPage the current page number
     * @param pageSize the page size
     * @param totalPages the total number of pages
     */
    public ResponseMetadata(Long totalRecords, Integer currentPage, Integer pageSize, Integer totalPages) {
        this();
        this.totalRecords = totalRecords;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    // Getters and Setters
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public LocalDateTime getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(LocalDateTime serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Object getCustomData() {
        return customData;
    }

    public void setCustomData(Object customData) {
        this.customData = customData;
    }

    @Override
    public String toString() {
        return "ResponseMetadata{" +
                "processingTimeMs=" + processingTimeMs +
                ", totalRecords=" + totalRecords +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                ", serverTimestamp=" + serverTimestamp +
                ", apiVersion='" + apiVersion + '\'' +
                ", customData=" + customData +
                '}';
    }
}
