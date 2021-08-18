package study.querydsl;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberTest {

	@Autowired
	EntityManager em;
	
	@Test
	void testEntity() {
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
		
		// initialize
		em.flush();
		em.clear();
		
		List<Member> members = em.createQuery("select m from Member m", Member.class)
								.getResultList();

		for(Member member : members) {
			System.out.println(member.getTeam());
		}
	}

}
