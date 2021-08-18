package study.querydsl;

import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.repository.MemberRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberRepositoryTest {

	@Autowired
	MemberRepository memberRepository;
	
	@Autowired
	private EntityManager em;
	
	@Test
	public void basicTest() {
		
		// save
		Member member1 = new Member("member1", 10);
		memberRepository.save(member1);
		
		// findById
		Member findMember = memberRepository.findById(member1.getId()).get();
		Assertions.assertThat(findMember).isEqualTo(member1);
		
		// findAll
		List<Member> result1 = memberRepository.findAll();
		Assertions.assertThat(result1).containsExactly(member1);
		
		// findByUsername
		List<Member> result2 = memberRepository.findByUsername("member1");
		Assertions.assertThat(result2).contains(member1);
	}

	@Test
	public void searchTest() {
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		
		em.persist(teamA);
		em.persist(teamB);
		
		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
		
		MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
		
		memberSearchCondition.setAgeGoe(20);
		memberSearchCondition.setAgeLoe(40);
		memberSearchCondition.setTeamName("teamB");
		
		List<MemberTeamDto> result = memberRepository.search(memberSearchCondition);
		
		Assertions.assertThat(result).extracting("username").containsExactly("member3", "member4");
	}
	
}
