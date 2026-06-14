package com.memoalgo.dto.response;

import com.memoalgo.entity.Topic;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse {

    private UUID id;
    private String name;
    private String declaration;
    private UUID parentTopicId;
    private String parentTopicName;

    public static TopicResponse fromEntity(Topic topic){
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .declaration(topic.getDescription())
                .parentTopicId(
                        topic.getParentTopic() != null ? topic.getParentTopic().getId() : null
                )
                .parentTopicName(
                        topic.getParentTopic() != null ? topic.getParentTopic().getName() : null
                )
                .build();
    }
}
