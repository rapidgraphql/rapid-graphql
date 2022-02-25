package org.rapidgraphql.app;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Repository {
    AtomicLong postId = new AtomicLong(1);
    private Map<Long, Post> posts = new ConcurrentHashMap<>();
    private Map<Long, List<Comment>> comments = new ConcurrentHashMap<>();
    public Long createPost(PostInput postInput) {
        Long currentPost = postId.getAndIncrement();
        Post post = Post.builder().id(currentPost).text(postInput.getText()).build();
        posts.put(currentPost, post);
//        if (postInput.getComments() != null) {
//            List<Comment> commentList = postInput.getComments().stream()
//                    .map(comment -> Comment.builder().id(currentPost).description(comment.getDescription()).build())
//                    .collect(Collectors.toList());
//            comments.merge(currentPost, commentList, (list1, list2) -> {list1.addAll(list2); return list1;});
//        }
        return currentPost;
    }

    public Post getPostById(Long postId) {
        return posts.get(postId);
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return comments.getOrDefault(postId, List.of());
    }

}
