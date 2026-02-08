package com.hibi.server.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * FAQ Entity (F9)
 */
@Entity
@Table(name = "faqs", indexes = {
        @Index(name = "idx_faqs_category", columnList = "category"),
        @Index(name = "idx_faqs_is_published", columnList = "is_published"),
        @Index(name = "idx_faqs_display_order", columnList = "display_order")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false, length = 500)
    private String question;

    @Column(name = "answer", nullable = false, length = 5000)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private FAQCategory category;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * FAQ 생성 팩토리 메서드
     */
    public static FAQ of(String question, String answer, FAQCategory category, Integer displayOrder) {
        return FAQ.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .isPublished(true)
                .build();
    }

    /**
     * FAQ 내용 업데이트
     */
    public void updateContent(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    /**
     * 카테고리 변경
     */
    public void updateCategory(FAQCategory category) {
        this.category = category;
    }

    /**
     * 정렬 순서 변경
     */
    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * 공개 상태 변경
     */
    public void publish() {
        this.isPublished = true;
    }

    /**
     * 비공개 상태 변경
     */
    public void unpublish() {
        this.isPublished = false;
    }
}
