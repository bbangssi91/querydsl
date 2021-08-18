package study.querydsl.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

public class MemberRepositoryImpl implements MemberRepositoryCustom{

	private final JPAQueryFactory queryFactory;
	
	public MemberRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}
	
	@Override
	public List<MemberTeamDto> search(MemberSearchCondition condition) {
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
