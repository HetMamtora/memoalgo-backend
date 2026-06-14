package com.memoalgo.service;

import com.memoalgo.dto.response.TopicResponse;
import com.memoalgo.entity.Topic;
import com.memoalgo.exception.ResourceNotFoundException;
import com.memoalgo.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicService {

    private final TopicRepository topicRepository;

    public List<TopicResponse> getAllTopics(){
        return topicRepository.findAll()
                .stream()
                .map(TopicResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TopicResponse getTopicById(UUID topicId){
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        return TopicResponse.fromEntity(topic);
    }
}
