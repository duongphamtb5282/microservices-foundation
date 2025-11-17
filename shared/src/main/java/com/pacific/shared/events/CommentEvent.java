package com.pacific.shared.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Comment-related events */
public class CommentEvent {

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class CommentCreated extends BaseEvent {
    private String commentId;
    private String content;
    private String authorId;
    private String authorName;
    private String parentCommentId;

    public CommentCreated() {
      super("CommentCreated", "comment-service", "1.0");
    }

    public CommentCreated(
        String commentId,
        String content,
        String authorId,
        String authorName,
        String parentCommentId) {
      super("CommentCreated", "comment-service", "1.0");
      this.commentId = commentId;
      this.content = content;
      this.authorId = authorId;
      this.authorName = authorName;
      this.parentCommentId = parentCommentId;
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class CommentUpdated extends BaseEvent {
    private String commentId;
    private String content;
    private String authorId;

    public CommentUpdated() {
      super("CommentUpdated", "comment-service", "1.0");
    }

    public CommentUpdated(String commentId, String content, String authorId) {
      super("CommentUpdated", "comment-service", "1.0");
      this.commentId = commentId;
      this.content = content;
      this.authorId = authorId;
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class CommentDeleted extends BaseEvent {
    private String commentId;
    private String authorId;

    public CommentDeleted() {
      super("CommentDeleted", "comment-service", "1.0");
    }

    public CommentDeleted(String commentId, String authorId) {
      super("CommentDeleted", "comment-service", "1.0");
      this.commentId = commentId;
      this.authorId = authorId;
    }
  }
}
