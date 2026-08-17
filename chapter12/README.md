# Chapter12. 스프링 데이터 JPA
## 1. 스프링 데이터 JPA 소개
객체지향적으로 코드를 작성하려면 **CRUD를 처리하기 위한 공통 인터페이스를 작성하자**

데이터 접근 계층을 개발할 때 구현 클래스 없이 인터페이스만 작성해도 개발할 수 있다.

`org.springframework.data.jpa.repository.JpaRepository` 인터페이스 이용

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByUsername(String username);
}

public interface ItemRepository extends JpaRepository<Item, Long> {
    
}
```
---
## 2. 스프링 데이터 JPA 설정

- 필요 라이브러리 : spring-data-jpa 라이브러리 추가
- 환경설정 : XML 사용 시 <jpa:repositories> 사용. 레포지토리를 검색할 base-package 적기
- JavaConfig 설정
```java
@Configuration
@EnableJpaRepositories(basePackages = "jpabook.jpashop.repository")
public class AppConfig {}
```

스프링 데이터 JPA는 애플리케이션을 실행할 때 basePackage에 있는 레포지토리 인터페이스들을 찾아 동적으로 생성한 후 스프링 빈으로 등록한다 (내가 직접 구현 클래스를 만들 필요 X)

---
## 3. 공통 인터페이스 기능

공통 인터페이스 구성

<img width="363" height="324" alt="image" src="https://github.com/user-attachments/assets/8e9ddb70-5187-4e75-81ae-4d21975f9645" />

주요 메서드

|메서드|기능|
|:---|:---|
save(S) | 새로운 엔티티는 저장하고 이미 있는 엔티티는 수정
delete(T) | 엔티티 하나를 삭제
findOne(ID) | 엔티티 하나를 조회
getOne(ID) | 엔티티를 프록시로 조회
findAll(...) | 모든 엔티티를 조회

---
## 4. 쿼리 메소드 기능
JPA가 제공하는 쿼리 메소드 기능

1. 메소드 이름으로 쿼리 생성
2. 메소드 이름으로 JPA NamedQuery 호출
3. @Query 어노테이션을 사용해서 레포지토리 인터페이스에 쿼리 직접 정의

### 4.1. 메소드 이름으로 쿼리 생성
이메일과 이름으로 회원을 조회
```java
public interface MemberRepository extends Repository<Member, Long> {
	List<Member> findByEmailAndName(String email, String name);
}
```
에서 findByEmailAndName(...) 메소드 호출하면

`select m from Member m where m.email = ?1 and m.name = ?2` 을 생성하고 실행한다.

스프링 데이터 JPA 쿼리 생성 기능은 더 찾아보자

### 4.2. JPA NamedQuery
메소드 이름으로 JPA Named 쿼리 호출

JPA NamedQuery : 쿼리에 이름을 부여해서 사용하는 방법

#### `@NamedQuery` 어노테이션으로 Named 쿼리 정의
```java
@Entity
@NamedQuery(
    name = "Member.findByUsername",
    query = "select m from member m where m.username = :username")
public class Member {
	...
}
```

#### 스프링 데이터 JPA를 사용하여 메소드 이름만으로 NamedQuery 호출하기 (간단!!)
```java
public interface UserRepository extends JpaRepository<Member, Long> {

    List<Member> findByUsername(@Param("username") String username);
}
```
### 4.3. @Query, 레포지토리 메소드에 쿼리 정의
실행할 메소드에 정적 쿼리를 직접 작성하는 방법 (이름 없는 NamedQuery 라고 할 수 있다.)

```java
public interface UserRepository extends JpaRepository<Member, Long> {

    @Query(value = "select * from member where username = ?0"
    	, nativeQuery = true)
    Member findByUsername(String username);
}
```

### 4.4. 파라미터 바인딩
**위치 기반** 파라미터 바인딩
```sql
select m from Member m where m.username = ?1
```

**이름 기반** 파라미터 바인딩
```sql
select m from Member m where m.username = :name
```

### 4.5. 벌크성 수정 쿼리
`@Modifying` 어노테이션 사용하면 된다.
```java
@Modifying
@Query("update Product p set p.price = p.price * 1.1 where p.stockAmount < :stockAmount")
int bulkPriceUp(@Param("stockAmount") String stockAmount);
```

벌크성 쿼리를 실행하고 나서 영속성 컨텍스트를 초기화하고 싶으면

`@Modifying(clearAutomatically = true)` (이 옵션의 기본값은 false)

### 4.6. 반환 타입
조회 결과가
- 한 건 이상 → 컬렉션 인터페이스 사용
- 단건 → 반환 타입 지정
```java
List<Member> findByName(String name); //컬렉션
Member findByEmail(String email); //단건
```

만약 조회 결과가 없다면, 빈 컬렌션/null 반환


단건에서 결과가 2개 이상 조회되면 NonUniqueResultException 예외 발생

### 4.7. 페이징과 정렬
- `org.springframework.data.domain.Sort` : 정렬 기능
- `org.springframework.data.domain.Pageable` : 페이징 기능(내부에 Sort 포함)

페이징과 정렬 사용 정의 예제 코드

```java
//count 쿼리 사용
Page<Member> findByName(String name, Pageable pageable);

//count 쿼리 사용 안 함
List<Member> findByName(String name, Pageable pageable);

List<Member> findByName(String name, Sort sort);
```

Page 사용 예제 실행 코드
```java
//페이징 조건과 정렬 조건 설정
PageRequest pagerequest = new PageRequest(0, 10, new Sort(Direction.DESC, "name"));

Page<Member> result = memberRepository.findByNameStartingWith("김", pageRequest);

List<Member> members = result.getContent(); //조회된 데이터
int totalPages = result.getTotalPages(); //전체 페이지 수
boolean hasNextPage = result.hasNextPage(); //다음 페이지 존재 여부
```

Page 인터페이스가 제공하는 메소드 더 찾아보기

### 4.8. 힌트
`@QueryHints` 어노테이션 사용하면 된다. (힌트는 JPA 구현체에게 제공하는 힌트임)
```java
@QueryHints(value = { @QueryHint(name = "org.hibernate.readOnly", value = "true")}
	, forCounting = true)
Page<Member> findByName(String name, Pageable pageable);
```

### 4.9. Lock
`@Lock` 어노테이션 사용하면 된다.
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Member> findByName(String name);
```
---
## 5. 명세
참, 거짓으로 평가되고 AND, OR 같은 연산자로 조합할 수 있는 것. 데이터를 검색하기 위한 제약 조건 하나하나를 술어라고 한다.

스프링 데이터 JPA는 `org.springframework.data.jpa.domain.Specification` 클래스로 정의했다.

명세를 사용하기 위해서는 `JpaSpecificationExecutor` 인터페이스를 상속받아야 한다.
```java
public interface OrderRepository extends JpaRepository<Order, Long>,
	JpaSpecificationExecutor<Order> {

}
```

명세 사용 코드
```java
public List<Order> findOrders(String name) {
	
    List<Order> result = orderRepository.findAll(
    	where(memberName(name)).and(isOrderStatus())
    );
    
    return result;
}
```

Specifications는 명세들을 조립할 수 있도록 도와주는 클래스이다.

→ `where()`, `and()`, `or()`, `not()` 메소드를 제공

---
## 6. 사용자 정의 레포지토리 구현
스프링 데이터 JPA로 레포지토리를 개발하면 구현체는 직접 만들지 않아도 되는데, 가끔 메소드를 직접 구현해야 할 때도 있다. (사용자 정의 인터페이스라고 부른다.)

- 인터페이스 이름 : 자유롭게 지으면 된다.
```java
public interface MemberRepositoryCustom {
	public List<Member> findMemberCustom();
}
```
- 사용자 정의 인터페이스를 구현한 클래스 이름 : **인터페이스 이름 + Impl** 로 지어야 한다.

```java
public class MemberRepositoryImpl implements MemberRepositoryCustom {
	
    @Override
    public List<Member> findMemberCustom() {
    	... //사용자 정의 구현
    }
}
```

이후 사용자 정의 인터페이스를 상속받으면 된다.
```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
```
---
## 7. Web 확장
### 7.1. 설정
스프링 데이터가 제공하는 Web 확장 기능을 활성화하려면
`org.springframework.data.web.config.SpringDataWebConfiguration` 을 스프링 빈으로 등록하면 된다.

JavaConfig를 사용하면 `@EnableSpringDataWebSupport` 어노테이션 사용하면 된다.

```java
@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
public class WebAppConfig {
	...
}
```

### 7.2. 도메인 클래스 컨버터 기능
도메인 클래스 컨버터는 HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩 해준다.

```java
@Controller
public class MemberController {
	
    @RequestMapping("member/memberUpdateForm")
    public String memberUpdateForm(@RequestParam("id") Member member, Model model) {
    	model.addAttribute("member", member);
        return "member/memberSaveForm";
    }
}
```
`@RequestParam("id") Member member` : HTTP 요청으로 회원 id를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체로 변환해서 넘겨준다.

회원 아이디로 회우너 엔티티 찾기!

### 7.3. 페이징과 정렬 기능
- 페이징 기능 : `PageableHandlerMethodArgumentResolver`
- 정렬 기능 : `SortHandlerMethodArgumentResolver`

```java
@RequestMapping(value = "/members", method = RequestMethod.GET)
public String list(pageable pageable, Model model) {
	
    Page<Member> page = memberService.findMembers(pageable);
    model.addAttribute("members", page.getContent());
    return "members/memberList";
}
```


Pageable의 요청 파라미터
- page: 현재 페이지, 0부터 시작
- size : 한 페이지에 노출할 데이터 건수
- sort : 정렬 조건 (ASC/DESC)

예) /members?page=0&size=20&sort=name,desc&sort=address.city

#### 접두사

사용해야 할 페이징 정보다 둘 이상이면 접두사를 사용하여 구분할 수 있다.
`@Qualifer` 어노테이션 사용해서 !
```java
public String list(
	@Qualifier("member") Pageable membePageable,
    @Qualifier("order") Pageable orderPageable,
    ...
```

`"{접두사명}_"` 으로 구분한다.
예) /members?member_page=0&order_page=1

#### 기본값
Pageable의 기본값은 page=0, size=20 이다.

---
## 8. 스프링 데이터 JPA가 사용하는 구현체
org.springframework.data.jpa.repository.support.SimpleJpaRepository

- `@Repository` 적용 : JPA 예외를 스프링이 추상화한 예외로 변환

- `@Transactional` 트랜잭션 적용 : JPA의 모든 변경은 트랜잭션 안에서 이루어져야 한다. 서비스 계층에서 트랜잭션을 시작하지 않으면 레포지토리에서 트랜잭션을 시작한다.

- `@Transactional(readOnly=true)` : 데이터를 변경하지 않는 트랜잭션에서 이 옵션을 이용하면 약간의 성능이 향상된다.

- `save()` 메소드 : 저장할 엔티티가 새로운 엔티티면 저장하고 / 이미 있는 엔티티면 병합한다.

---
## 10. 스프링 데이터 JPA와 QueryDSL 통합
### 10.1. QueryDslPredicateExecutor 사용
QueryDslPredicateExecutor를 상속받는다.
```java
public interface ItemRepository extends JpaRepository<Item, Long>, 
	QueryDslPredicateExecutor<Item> {

}
```

이제 상품 레포지토리에 QueryDSL을 사용할 수 있다.
```java
QItem item = QItem.item;
Iterable<Item> result = itemRepository.findAll(
	item.name.contains("장난감").and(item.price.between(10000, 20000))
);
```

### 10.2. QueryDslRepositorySupport 사용
QueryDSL이 제공하는 더 다양한 기능을 사용하려면 JPAQuery를 직접 사용하거나 스프링 데이터 JPA가 제공하는 QueryDslRepositorySupport를 사용해야 한다.

```java
public class OrderRepositoryImpl extends QueryDslRepositorySupport 
	implements CustomOrderRepository {
	...
}
```
