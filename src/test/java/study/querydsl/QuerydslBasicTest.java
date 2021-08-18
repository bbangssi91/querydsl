package study.querydsl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class QuerydslBasicTest {

	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory;
	
	// Runconfig :: -DSpring.profiles.active=test
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
		
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
	}
	
	@Test
	public void startJPQL() {
		// member1을 찾아라.
		Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
						.setParameter("username", "member1")
						.getSingleResult();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {
		QMember m = new QMember("m");
		
		Member findMember = queryFactory
						.select(m)
						.from(m)
						.where(m.username.eq("member1"))
						.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void search() {
		QMember m = new QMember("m");
		
		Member findMember = queryFactory
						.selectFrom(m)
						.where(
							m.username.eq("member1")
							.and(m.age.eq(10))
						)
						.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		Assertions.assertThat(findMember.getAge()).isEqualTo(10);
	}
	
	@Test
	public void searchAndParam() {
		QMember m = new QMember("m");
		
		Member findMember = queryFactory
						.selectFrom(m)
						.where(
							m.username.eq("member1"),
							m.age.between(10, 30)
						)
						.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		Assertions.assertThat(findMember.getAge()).isEqualTo(10);
	}
	
	@Test
	public void resultFetch() {
		QMember m = new QMember("m");
		/*
		// 1. select Members
		List<Member> fetch = queryFactory
							.selectFrom(m)
							.fetch();
		
		// 2. select Member
		Member fetchOne = queryFactory
						.selectFrom(m)
						.limit(1)
						.fetchOne();
		
		// 3. select first Member
		Member fetchFirst = queryFactory
						.selectFrom(m)
						.fetchFirst();
		*/
		// 4. select results 
		QueryResults<Member> results = queryFactory
						.selectFrom(m)
						.fetchResults();
		
		results.getTotal();
		List<Member> content = results.getResults();
		System.out.println("===> " + content);
	}
	
	/**
	 * 정렬조건
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단, 2에서 회원이름이 없으면 마지막에 출력 (nullsLast)
	 */
	@Test
	public void sort() {
		QMember m = new QMember("m");
		
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		
		List<Member>  result = queryFactory
						.selectFrom(m)
						.where(m.age.eq(100))
						.orderBy(m.age.desc(), m.username.asc().nullsLast())
						.fetch();
		
		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member member7 = result.get(2);
		
		Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
		Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
		Assertions.assertThat(member7.getUsername()).isNull();
	}
	
	@Test
	public void paging1() {
		QMember m = QMember.member;
		
		List<Member> result = queryFactory.selectFrom(m)
						.orderBy(m.username.desc())
						.offset(1)
						.limit(2)
						.fetch();
		
		Assertions.assertThat(result.size()).isEqualTo(2);
	}
	
	@Test
	public void paging2() {
		QMember m = QMember.member;
		
		QueryResults<Member> result = queryFactory
						.selectFrom(m)
						.orderBy(m.username.desc())
						.offset(1)
						.limit(2)
						.fetchResults();
		
		Assertions.assertThat(result.getTotal()).isEqualTo(4); // 총 4개
		Assertions.assertThat(result.getLimit()).isEqualTo(2); // 제한2
		Assertions.assertThat(result.getOffset()).isEqualTo(1); // 오프셋1
		Assertions.assertThat(result.getResults().size()).isEqualTo(2); // 페이징 처리결과 2개
	}
	
	@Test
	public void aggregation() {
		QMember m = QMember.member;
		
		List<Tuple> result = queryFactory
						.select(
								m.count(),
								m.age.sum(),
								m.age.avg(),
								m.age.max(),
								m.age.min()
						)
						.from(m)
						.fetch();
		
		Tuple tuple = result.get(0);
		
		Assertions.assertThat(tuple.get(m.count())).isEqualTo(4);
		Assertions.assertThat(tuple.get(m.age.sum())).isEqualTo(100);
		Assertions.assertThat(tuple.get(m.age.avg())).isEqualTo(25);
		Assertions.assertThat(tuple.get(m.age.max())).isEqualTo(40);
		Assertions.assertThat(tuple.get(m.age.min())).isEqualTo(10);
	}
	
	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라
	 * 
	 * 
	 */
	
	@Test
	public void group() throws Exception{
		QMember m = QMember.member;
		QTeam t = QTeam.team;
		
		List<Tuple> result = queryFactory
						.select(t.name, m.age.avg())
						.from(m)
						.join(m.team, t)
						.groupBy(t.name)
						.fetch();
		
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		
		Assertions.assertThat(teamA.get(t.name)).isEqualTo("teamA");
		Assertions.assertThat(teamA.get(m.age.avg())).isEqualTo(15);
		
		Assertions.assertThat(teamB.get(t.name)).isEqualTo("teamB");
		Assertions.assertThat(teamB.get(m.age.avg())).isEqualTo(35);
	}
	
	/**
	 * 팀 A에 소속된 모든 회원을 조회하라.
	 */
	@Test
	public void join() {
		QMember m = QMember.member;
		QTeam t = QTeam.team;
		
		List<Member> result = queryFactory
						.selectFrom(m)
						.join(m.team, t)
						.where(t.name.eq("teamA"))
						.fetch();
		
		Assertions.assertThat(result)
				.extracting("username")
				.containsExactly("member1", "member2");
	}
	
	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회한다.
	 */
	@Test
	public void join_on_filtering() {
		QMember m = QMember.member;
		QTeam t = QTeam.team;

		List<Tuple> result = queryFactory
						.select(m, t)
						.from(m)
						.leftJoin(m.team, t).on(t.name.eq("teamA"))
						.fetch();
		
		for(Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
		
	}
	
	@PersistenceUnit
	EntityManagerFactory emf;
	
	@Test
	public void noFetchJoin() {
		QMember m = QMember.member;
		
		// persistence-context detach
		em.flush();
		em.clear();
		
		// * Team 엔티티는 LazyLoading으로 셋팅되어있음
		Member findMember = queryFactory
						.selectFrom(m)
						.where(m.username.eq("member1"))
						.fetchOne();
		
		boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		
		Assertions.assertThat(isLoaded).as("fetchjoin 미적용").isFalse();
	}
	
	@Test
	public void FetchJoin() {
		QMember m = QMember.member;
		QTeam t = QTeam.team;
		
		// persistence-context detach
		em.flush();
		em.clear();
		
		// * Team 엔티티는 LazyLoading으로 셋팅되어있음
		Member findMember = queryFactory
						.selectFrom(m)
						.join(m.team, t).fetchJoin()
						.where(m.username.eq("member1"))
						.fetchOne();
		
		boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		
		Assertions.assertThat(isLoaded).as("fetchjoin 적용").isTrue();
	}
	
	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	public void subQuery() {
		QMember m = QMember.member;
		QMember subM = new QMember("subM");
		
		List<Member> result = queryFactory
						.selectFrom(m)
						.where(m.age.eq(
								JPAExpressions
									.select(subM.age.max())
									.from(subM)
						))
						.fetch();
		
		Assertions.assertThat(result).extracting("age")
								.containsExactly(40);
	}
	
	/**
	 * 나이가 평균 이상인 회원
	 */
	@Test
	public void subQueryGoe() {
		QMember m = QMember.member;
		QMember subM = new QMember("subM");
		
		List<Member> result = queryFactory
						.selectFrom(m)
						.where(m.age.goe(
								JPAExpressions
									.select(subM.age.avg())
									.from(subM)
						))
						.fetch();
		
		Assertions.assertThat(result).extracting("age")
								.containsExactly(30, 40);
	}
	
	@Test
	public void subQueryIn() {
		QMember m = QMember.member;
		QMember subM = new QMember("subM");
		
		List<Member> result = queryFactory
						.selectFrom(m)
						.where(m.age.in(
								JPAExpressions
									.select(subM.age)
									.from(subM)
									.where(subM.age.gt(10))
						))
						.fetch();
		
		Assertions.assertThat(result).extracting("age")
								.containsExactly(20, 30, 40);
	}
	
	@Test
	public void basicCase() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(m.age
						.when(10).then("열살")
						.when(20).then("스무살")
						.otherwise("기타"))
					.from(m)
					.fetch();
		
		for(String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void complexCase() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(new CaseBuilder()
								.when(m.age.between(0, 20)).then("0~20살")
								.when(m.age.between(21, 30)).then("21~30살")
								.otherwise("기타"))
					.from(m)
					.fetch();
		
		for(String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void constant() {
		QMember m = QMember.member;
		
		List<Tuple> result = queryFactory
						.select(m.username, Expressions.constant("A"))
						.from(m)
						.fetch();
		
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}
	
	/**
	 * {username}_{age}
	 */
	@Test
	public void concat() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(m.username.concat("_").concat(m.age.stringValue()))
						.from(m)
						.where(m.username.eq("member1"))
						.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void simpleProjection() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(m.username)
						.from(m)
						.fetch();
		
		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void tupleProjection() {
		QMember m = QMember.member;
		
		List<Tuple> result = queryFactory
						.select(m.username, m.age)
						.from(m)
						.fetch();

		for (Tuple tuple : result) {
			String username = tuple.get(m.username);
			Integer age = tuple.get(m.age);
			
			System.out.println("username = " + username);
			System.out.println("age = " + age);
		}
	}
	
	@Test
	public void findfDtoByJPQL() {
		List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m"
										, MemberDto.class)
										.getResultList();
		
		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	// default constructor -> setter -> update field
	@Test
	public void findDtoBySetter() {
		QMember m = QMember.member;
		
		List<MemberDto> result = queryFactory
						.select(
							Projections.bean(MemberDto.class,
									m.username
									, m.age))
						.from(m)
						.fetch();
		
		for (MemberDto memberDto : result) {
			System.out.println("Projection setter MemberDto = " + memberDto);
		}
	}
	
	// update field directly
	@Test
	public void findDtoByField() {
		QMember m = QMember.member;
		
		List<MemberDto> result = queryFactory
						.select(
							Projections.fields(MemberDto.class,
									m.username
									, m.age))
						.from(m)
						.fetch();
		
		for (MemberDto memberDto : result) {
			System.out.println("Projection field MemberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoByConstructor() {
		QMember m = QMember.member;
		
		List<MemberDto> result = queryFactory
						.select(
							Projections.constructor(MemberDto.class,
									m.username
									, m.age))
						.from(m)
						.fetch();
		
		for (MemberDto memberDto : result) {
			System.out.println("Projection constructor MemberDto = " + memberDto);
		}
	}

	@Test
	public void findUserDtoByField() {
		QMember m = QMember.member;
		QMember subM = new QMember("subM");
		
		List<UserDto> result = queryFactory
						.select(
							Projections.fields(UserDto.class,
									m.username.as("name"),
									ExpressionUtils.as(JPAExpressions
													.select(subM.age.max())
													.from(subM), "age")
								))
						.from(m)
						.fetch();
		
		for (UserDto userDto: result) {
			System.out.println("Projection field MemberDto = " + userDto);
		}
	}
	
	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = "member1";
		Integer ageParam = null;
		
		List<Member> result = searchMember1(usernameParam, ageParam);
		
		Assertions.assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {
		QMember m = QMember.member;
		
		BooleanBuilder builder = new BooleanBuilder();
		
		if(usernameCond != null) {
			builder.and(m.username.eq(usernameCond));
		}
		
		if(ageCond != null) {
			builder.and(m.age.eq(ageCond));
		}
		
		return queryFactory
				.selectFrom(m)
				.where(builder)
				.fetch();
	}
	
	@Test
	public void dynamicQuery_WhereParam() {
		String usernameParam = "member1";
		Integer ageParam = 10;
		
		List<Member> result = searchMember2(usernameParam, ageParam);
		
		Assertions.assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		QMember m = QMember.member;
		
		return queryFactory
						.selectFrom(m)
						.where(usernameEq(usernameCond),
								ageEq(ageCond), 
								allEq(usernameCond, ageCond))
						.fetch();
	}


	private BooleanExpression usernameEq(String usernameCond) {
		QMember m = QMember.member;
				
		return usernameCond != null? m.username.eq(usernameCond) : null; 
	}
	
	private BooleanExpression ageEq(Integer ageCond) {
		QMember m = QMember.member;

		return ageCond != null? m.age.eq(ageCond) : null;
	}
	
	private BooleanExpression allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}

	/**
	 * member1 = 10 -> 비회원  변경
	 * member2 = 20 -> 비회원 변경
	 * member3 = 30 -> 유지
	 * member4 = 40 -> 유지
	 */
	
	@Test
	public void bulkUpdate() {
		QMember m = QMember.member;

		long count = queryFactory
						.update(m)
						.set(m.username, "비회원")
						.where(m.age.lt(28))
						.execute();
		
		// 벌크연산의 경우, persistence-context를 거치지 않고 DB로 바로 붙는다.
		// 이 상태에서 엔티티를 조회를 하게되면, 1차캐시와 DB데이터가 동기화되지 않는 문제가 발생한다.
		// 따라서 동기화를 위해 1차캐시를 초기화하여 다시 DB에서 조회해오도록 한다.
		
		em.flush();
		em.clear();
		
		List<Member> result = queryFactory
						.selectFrom(m)
						.fetch();
		
		for (Member member : result) {
			System.out.println("member = " + member);
		}
		
		Assertions.assertThat(count).isEqualTo(2);
		
	}
	
	@Test
	public void bulkAdd() {
		QMember m = QMember.member;
		
		queryFactory
				.update(m)
				.set(m.age, m.age.add(1))
				.execute();
	}
	
	@Test
	public void bulkDelete() {
		QMember m = QMember.member;
		
		queryFactory
				.delete(m)
				.where(m.age.gt(18))
				.execute();
	}
	
	@Test
	public void sqlFunction() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(
								Expressions.stringTemplate(
										"function('replace', {0}, {1}, {2})", 
										m.username, "member", "M"))
						.from(m)
						.fetch();
				
		for(String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	public void sqlFunction2() {
		QMember m = QMember.member;
		
		List<String> result = queryFactory
						.select(m.username)
						.from(m)
//						.where(m.username.eq(
//								Expressions.stringTemplate("function('lower', {0})", m.username)))
						.where(m.username.eq(m.username.lower()))
						.fetch();

		for(String s : result) {
			System.out.println("s = " + s);
		}
	}
	
}
