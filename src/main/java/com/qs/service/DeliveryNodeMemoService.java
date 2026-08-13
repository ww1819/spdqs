package com.qs.service;

import com.qs.entity.DeliveryNode;
import com.qs.entity.DeliveryNodeMemo;
import com.qs.repository.DeliveryNodeMemoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryNodeMemoService {

    private final DeliveryNodeMemoRepository memoRepository;

    public DeliveryNodeMemoService(DeliveryNodeMemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    public long countByNodeId(String nodeId) {
        return memoRepository.countByNodeId(nodeId);
    }

    public List<DeliveryNodeMemo> listByNodeId(String nodeId) {
        return memoRepository.findByNodeIdOrderByCreateTimeDesc(nodeId);
    }

    public DeliveryNodeMemo getById(String id) {
        return memoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("备忘录不存在"));
    }

    @Transactional
    public DeliveryNodeMemo create(DeliveryNode node, String content, String createBy) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("请填写备忘内容");
        }
        DeliveryNodeMemo memo = new DeliveryNodeMemo();
        memo.setNodeId(node.getId());
        memo.setDeliveryId(node.getDeliveryId());
        memo.setContent(content.trim());
        memo.setCreateBy(createBy);
        return memoRepository.save(memo);
    }

    @Transactional
    public void confirm(String memoId, String confirmedBy) {
        DeliveryNodeMemo memo = getById(memoId);
        if (memo.isConfirmed()) {
            return;
        }
        memo.setConfirmed(true);
        memo.setConfirmedBy(confirmedBy);
        memo.setConfirmedTime(LocalDateTime.now());
        memoRepository.save(memo);
    }

    @Transactional
    public void delete(String memoId) {
        DeliveryNodeMemo memo = getById(memoId);
        if (memo.isConfirmed()) {
            throw new IllegalArgumentException("备忘录已确认，不可删除");
        }
        memoRepository.delete(memo);
    }

    @Transactional
    public void deleteByNodeId(String nodeId) {
        memoRepository.deleteByNodeId(nodeId);
    }
}
