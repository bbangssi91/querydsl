package study.querydsl.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

@Repository
public class MemberJpaRepository {

	private final EntityManager em;
	private final JPAQueryFactory queryFactory;
	
	public MemberJpaRepository(EntityManager em) {
		this.em = em;
		this.queryFactory = new JPAQueryFactory(em);
	}
	
	public void save(Member member) {
		em.persist(member);
	}
	
	public Optional<Member> findById(Long id){
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
		
	}
	
	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class)
				.getResultList();
	}
	
	public List<Member> findAll_Querydsl(){
		QMember m = QMember.member;
		
		return queryFactory
				.selectFrom(m)
				.fetch();
	}	
	
	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m "
							+ "where m.username = :username", Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<Member> findByUsername_Querydsl(String username) {
		QMember m = QMember.member;
		
		return queryFactory
				.selectFrom(m)
				.where(m.username.eq(username))
				.fetch();
	}
	
	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition searchCondition){
		QMember m = QMember.member;
		QTeam t = QTeam.team;

		BooleanBuilder builder = new BooleanBuilder();
		
		if(StringUtils.hasText(searchCondition.getUsername())) {
			builder.and(m.username.eq(searchCondition.getUsername()));
		}
		if(StringUtils.hasText(searchCondition.getTeamName())) {
			builder.and(t.name.eq(searchCondition.getTeamName()));
		}
		if(searchCondition.getAgeGoe() != null) {
			builder.and(m.age.goe(searchCondition.getAgeGoe()));
		}
		if(searchCondition.getAgeLoe() != null) {
			builder.and(m.age.loe(searchCondition.getAgeLoe()));
		}
		
		return queryFactory
				.select(new QMemberTeamDto(
						m.id.as("memberId"),
						m.username,
						m.age,
						t.id.as("teamId"),
						t.name.as("teamName")
						))
				.from(m)
				.leftJoin(m.team, t)
				.where(builder)
				.fetch();
	}
	
	
	public List<MemberTeamDto> search(MemberSearchCondition condition){
		QMember m = QMember.member;
		QTeam t = QTeam.team;
		
		return queryFactory
				.select(new QMemberTeamDto(
						m.id.as("memberId"),
						m.username,
						m.age,
						t.id.as("teamId"),
						t.name.as("teamName")
						))
				.from(m)
				.leftJoin(m.team, t)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						)
				.fetch();
	}

	private BooleanExpression usernameEq(String username) {
		QMember m = QMember.member;
		return StringUtils.hasText(username) ? m.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		QTeam t = QTeam.team;
		return StringUtils.hasText(teamName) ? t.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		QMember m = QMember.member;
		return ageGoe != null ? m.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		QMember m = QMember.member;
		return ageLoe != null ? m.age.loe(ageLoe) : null;
	}
	
}
