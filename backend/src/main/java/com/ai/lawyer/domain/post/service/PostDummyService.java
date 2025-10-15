package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.Member.Gender;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.repository.PollOptionsRepository;
import com.ai.lawyer.domain.poll.repository.PollVoteRepository;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PostDummyService {
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PollOptionsRepository pollOptionsRepository;
    private final PollVoteRepository pollVoteRepository;

    @Autowired
    public PostDummyService(MemberRepository memberRepository,
                            PostRepository postRepository,
                            PollOptionsRepository pollOptionsRepository,
                            PollVoteRepository pollVoteRepository) {
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.pollOptionsRepository = pollOptionsRepository;
        this.pollVoteRepository = pollVoteRepository;
    }

    @Transactional
    public int createDummyMembers(int count) {
        List<Member> allMembers = memberRepository.findAll();
        int maxDummyNumber = allMembers.stream()
                .map(Member::getLoginId)
                .filter(id -> id.startsWith("dummy") && id.endsWith("@test.com"))
                .map(id -> {
                    try {
                        String numStr = id.substring(5, id.indexOf("@"));
                        return Integer.parseInt(numStr);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);
        int start = maxDummyNumber + 1;
        int end = start + count - 1;
        List<String> newLoginIds = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            newLoginIds.add("dummy" + i + "@test.com");
        }
        List<Member> existingMembers = memberRepository.findByLoginIdIn(newLoginIds);
        Set<String> existingLoginIds = new HashSet<>();
        for (Member m : existingMembers) {
            existingLoginIds.add(m.getLoginId());
        }
        List<Member> membersToSave = new ArrayList<>();
        Random random = new Random();
        for (int i = start; i <= end; i++) {
            String loginId = "dummy" + i + "@test.com";
            if (!existingLoginIds.contains(loginId)) {
                int age = 14 + random.nextInt(67);
                Gender gender = (i % 2 == 0) ? Gender.MALE : Gender.FEMALE;
                Member member = Member.builder()
                        .loginId(loginId)
                        .password("password")
                        .age(age)
                        .gender(gender)
                        .name("투표자" + i)
                        .build();
                membersToSave.add(member);
            }
        }
        if (!membersToSave.isEmpty()) {
            memberRepository.saveAll(membersToSave);
        }
        return membersToSave.size();
    }

    @Transactional
    public int dummyVote(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) return 0;
        Post post = postOpt.get();
        if (post.getPoll() == null) return 0;
        List<PollOptions> pollOptionsList = pollOptionsRepository.findByPoll_PollId(post.getPoll().getPollId());
        if (pollOptionsList.isEmpty()) return 0;
        // 모든 멤버 조회 후 더미 멤버만 필터링
        List<Member> dummyMembers = memberRepository.findAll().stream()
                .filter(m -> m.getLoginId().startsWith("dummy") && m.getLoginId().endsWith("@test.com"))
                .toList();
        List<Long> votedMemberIds = pollVoteRepository.findMemberIdsByPoll(post.getPoll());
        Set<Long> votedMemberIdSet = new HashSet<>(votedMemberIds);
        int voteCount = 0;
        Random random = new Random();
        for (Member member : dummyMembers) {
            if (!votedMemberIdSet.contains(member.getMemberId())) {
                PollOptions selectedOption = pollOptionsList.get(random.nextInt(pollOptionsList.size()));
                PollVote pollVote = PollVote.builder()
                        .poll(post.getPoll())
                        .member(member)
                        .pollOptions(selectedOption)
                        .build();
                pollVoteRepository.save(pollVote);
                voteCount++;
            }
        }
        return voteCount;
    }

    @Transactional
    public int deleteDummyMembers() {
        List<Member> dummyMembers = memberRepository.findAll().stream()
                .filter(m -> m.getLoginId().startsWith("dummy") && m.getLoginId().endsWith("@test.com"))
                .toList();
        int count = dummyMembers.size();
        if (count > 0) {
            for (Member member : dummyMembers) {
                pollVoteRepository.deleteByMemberIdValue(member.getMemberId());
            }
            memberRepository.deleteAll(dummyMembers);
        }
        return count;
    }
}
