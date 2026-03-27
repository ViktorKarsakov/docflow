package kkkvd.docflow.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "substitutions")
@Getter
@Setter
@NoArgsConstructor
public class Substitution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_user_id", nullable = false)
    private User substituteUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_user_id", nullable = false)
    private User originalUser;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 300)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        return active
                && !today.isBefore(startDate)
                && !today.isAfter(endDate);
    }
}
