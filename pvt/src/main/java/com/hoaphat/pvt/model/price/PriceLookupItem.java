package com.hoaphat.pvt.model.price;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "price_lookup_item", indexes = {
        @Index(name = "idx_price_lookup_pending", columnList = "lookup_status,assigned_account_name"),
        @Index(name = "idx_price_lookup_completed", columnList = "completed_at")
})
public class PriceLookupItem {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private PriceLookupBatch batch;

    @Column(name = "keyword_text", nullable = false, length = 1000)
    private String keyword;

    @Column(name = "display_name", length = 1000)
    private String displayName;

    @Column(name = "original_line", nullable = false, length = 2000)
    private String originalLine;

    @Column(name = "assigned_account_name", nullable = false, length = 100)
    private String assignedAccountName;

    @Column(name = "assigned_display_name", nullable = false, length = 200)
    private String assignedDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "lookup_status", nullable = false, length = 40)
    private PriceLookupStatus status;

    @Column(name = "status_message", length = 1000)
    private String statusMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("candidateId ASC")
    private List<PriceLookupCandidate> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("groupNumber ASC")
    private List<PriceLookupResult> results = new ArrayList<>();

    public void addCandidate(PriceLookupCandidate candidate) {
        candidate.setItem(this);
        candidates.add(candidate);
    }

    public void addResult(PriceLookupResult result) {
        result.setItem(this);
        results.add(result);
    }

    public String getCreatedAtDisplay() { return createdAt == null ? "" : createdAt.format(DISPLAY_TIME); }
    public String getCompletedAtDisplay() { return completedAt == null ? "" : completedAt.format(DISPLAY_TIME); }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public PriceLookupBatch getBatch() { return batch; }
    public void setBatch(PriceLookupBatch batch) { this.batch = batch; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getDisplayName() {
        return displayName == null || displayName.trim().isEmpty() ? keyword : displayName;
    }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getOriginalLine() { return originalLine; }
    public void setOriginalLine(String originalLine) { this.originalLine = originalLine; }
    public String getAssignedAccountName() { return assignedAccountName; }
    public void setAssignedAccountName(String assignedAccountName) { this.assignedAccountName = assignedAccountName; }
    public String getAssignedDisplayName() { return assignedDisplayName; }
    public void setAssignedDisplayName(String assignedDisplayName) { this.assignedDisplayName = assignedDisplayName; }
    public PriceLookupStatus getStatus() { return status; }
    public void setStatus(PriceLookupStatus status) { this.status = status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public List<PriceLookupCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<PriceLookupCandidate> candidates) { this.candidates = candidates; }
    public List<PriceLookupResult> getResults() { return results; }
    public void setResults(List<PriceLookupResult> results) { this.results = results; }
}
