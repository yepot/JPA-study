# Chpater05. 연관관계 매핑 기초
[ 연관관계가 필요한 이유 ]

객체지향적인 설계는 자율적인 객체들의 협력을 가능하게 하는 설계를 의미하는데

객체를 테이블에 맞추어 데이터 중심으로 모델링하면, 협력 관계를 만들 수 없다.
- 테이블은 외래키로 조인을 사용해서 연관된 테이블을 찾음
- 객체는 참조를 사용해서 연관된 객체를 찾음

테이블과 객체 사이에는 이런 차이점이 있었다.

## 1. 단방향 연관관계
### 1.1 단방향 연관관계
> 단방향 연관관계: 한 객체가 다른 객체를 참조하지만, 반대 방향의 참조는 없는 관계

객체는 참조를 통해 연관 객체에 접근하고, JPA는 참조를 외래키로 변환해준다.

#### ORM 매핑 방식
`@ManyToOne`, `@JoinColumn(name = "TEAM_ID")` 등을 사용하여 외래 키와 객체 참조를 연결

예) Member가 Team을 참조하지만 Team은 Member를 알지 못한다.

#### 장점
단순한 구조, 설계 및 구현이 간편하고, 테이블 설계와 유사하게 매핑 가능하다.

#### 사용 예
```java
@ManyToOne
@JoinColumn(name = "TEAM_ID")
private Team team;
```
`member.setTeam(team);` //단방향 연관관계 설정

### 1.2 객체 지향 모델링
<img width="320" height="180" alt="image" src="https://github.com/user-attachments/assets/ece10301-affb-4794-9c13-ab62cf773905" />

#### 객체의 참조와 테이블의 외래키 매핑
Member
```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    @Column(name="MEMBER_ID")
    private Long id;

    @Column(name="USERNAME")
    private String username;

//    @Column(name="TEAM_ID")
//    private Long teamId;

    //DB관점에서 어떤 관계인지 알려줘야한다!
    @ManyToOne
    @JoinColumn(name="TEAM_ID")
    private Team team;
```

#### 연관관계 저장
JpaMain
```java
//팀 저장
Team team=new Team();
team.setName("TeamA");
em.persist(team);

//회원 저장
Member member=new Member();
member.setUsername("member1");
member.setTeam(team); //단방향 연관관계 설정, 참조 저장 !!
em.persist(member);
            
em.flush(); //DB에 쿼리를 즉시 반영
em.clear(); //현재까지의 JPA 관리 상태를 초기화
 ```
#### 참조로 연관관계 조회 - 객체 그래프 탐색
 ```java
//한번에 땡겨오기
//Member findMember=em.find(Member.class, member.getId());

//조회
Member findMember = em.find(Member.class, member.getId());
//참조를 사용해서 연관관계 조회(바로 끄집어내서 쓸 수 있음)
Team findTeam = findMember.getTeam();
```
참조가 아니라 테이블 값에 맞추어서 그대로 저장하게 된다.

#### 객체 연관관계 사용

```java
 @ManyToOne
 @JoinColumn(name="TEAM_ID")
 private Team team;
 ```
 관계의 종류와 조인할 외래키 컬럼만 명시해주면 → JPA가 자동으로 매핑을 처리한다.
 
#### JPA의 자동 처리 흐름
1. `member.setTeam(team);` : JPA는 내부적으로 team.getId() 값을 가져와서 외래 키에 세팅한다.
3. `em.persist(member);` : INSERT 시 TEAM_ID 컬럼에 외래 키 값이 자동으로 들어간다.

## 2. 양방향 연관관계와 연관관계의 주인 - 기본
### 2.1 양방향 연관관계
> 양방향 연관관계 : 양쪽 객체가 서로를 참조하는 관계

Member가 Team을 참조하고, Team도 Member 목록을 가진다.
#### 구현 방식
`Member.team`은 `@ManyToOne`으로 외래 키를 직접 매핑한다.

`Team.members`는 `@OneToMany(mappedBy = "team")`으로 반대 방향을 매핑한다.

#### 예시 코드
```java
// Member.java
@ManyToOne
@JoinColumn(name = "TEAM_ID")
private Team team;

// Team.java
@OneToMany(mappedBy = "team")
private List<Member> members = new ArrayList<>();
```

### 2.2 양방향 매핑
<img width="354" height="208" alt="image" src="https://github.com/user-attachments/assets/2b8e922c-8e1e-4caf-99f2-ce4ebe72b08f" />

#### 객체 vs 테이블의 연관관계
- 테이블 : `TEAM_ID` 외래키 하나만 있으면 [회원 → 팀 / 팀 → 회원] 양쪽 조회 가능
- 객체 : 객체 참조는 단방향

→ [Member → Team] 은 참조 가능하지만, [Team → Member] 는 불가능

→ 해결 방법 : `Team` 엔티티에 `List<Member> members` 를 추가해 양방향 참조 구현

```java
@OneToMany(mappedBy = "team")
private List<Member> members = new ArrayList<>();
```

#### Team 엔티티는 컬렌션 추가
```java
@Entity
public class Team {

@Id
@GeneratedValue
@Column(name="TEAM_ID")
private Long id;
private String name;

@OneToMany(mappedBy = "team")
private List<Member> members = new ArrayList<>();
```
#### 반대 방향으로 그래프 탐색
 
```java
Member findMember=em.find(Member.class, member.getId());
            List<Member> members=findMember.getTeam().getMembers();

            for(Member m : members){
                System.out.println("m = "+ m.getUsername());
            }
```

#### 연관관계 주인과 mappedBy
양방향 일 때, 객체 기준으로는 단방향 관계가 2개면 JPA에서는 이 중 한 쪽만 외래키 관리 주인이 되어야 한다.

|구분|연관관계 수|비고|
|:---|:---|:---|
|객체|2개|각 객체가 서로 참조|
|테이블|1개|외래키 하나로 조인 가능|
|연관관계의 주인|외래키가 있는 쪽 (@JoinColumn 사용)|`mappedBy` 는 주인이 아님을 명시

#### 테이블의 양방향 관계
테이블은 외래키 하나만으로 양방향 조인이 가능하다.

예) `MEMBER.TEAM_ID` 라는 외래 키만 있어도, [MEMBER → TEAM] 조회 가능, [TEAM → MEMBER] 조회 가능

SQL 예시
```sql
-- 회원에서 팀으로
SELECT *
FROM MEMBER M
JOIN TEAM T ON M.TEAM_ID = T.TEAM_ID;
  
-- 팀에서 회원으로
SELECT *
FROM TEAM T
JOIN MEMBER M ON T.TEAM_ID = M.TEAM_ID;
```

## 3. 양방향 연관관계와 연관관계의 주인 - 주의점, 정리
둘 중 하나로 외래 키를 관리해야 한다!
<img width="359" height="180" alt="image" src="https://github.com/user-attachments/assets/53b29e4a-b5bb-46b7-8f91-0bacb08baaf7" />

#### 양방향 매핑의 규칙
- 객체의 두 관계 중 하나를 연관관계의 주인으로 지정해야 한다.
- 연관관계의 주인만 외래 키를 등록, 수정(관리) 할 수 있다.
- 주인이 아닌 쪽은 읽기 전용이며, DB의 연관관계에 영향을 주지 않는다.

|구분|사용여부|
|:---|:---|
|주인|mappedBy 사용하지 않음|
|비주인|	mappedBy로 주인 지정|

#### 누가 주인인가?
- 기준 : 외래 키가 있는 곳이 연관관계의 주인
- 보통 `다대일(N:1)` 관계에서 `N 쪽`이 외래키를 가지고 있어 주인이 된다.
  
예시
```java
@Entity
public class Member {
    @ManyToOne
    @JoinColumn(name = "TEAM_ID") // 외래 키 존재 → 연관관계의 주인
    private Team team;
}

@Entity
public class Team {
    @OneToMany(mappedBy = "team") // 외래 키 없음 → 주인 아님
    private List<Member> members = new ArrayList<>();
}
```

#### 양방향 연관관계 주의 - 실습 팁

연관관계의 주인에 값을 입력하지 않음

![](https://velog.velcdn.com/images/yepot/post/7adf9c04-c15d-415f-8318-a1b96ee0faa3/image.png)

이렇게 순서를 바꾸면 인서트 쿼리는 분명히 두 번 실행됐다. 근데 TEAM_ID가 null 이다..

양방향 매핑시 연관관계의 주인에 값을 입력해야 한다. (항상 양쪽 다 값을 입력해야 한다.)

순수 객체 상태에서는 `member.setTeam(team)` 만 해서는 `team.getMembers()` 에서 값이 안 보일 수 있다.

따라서 양쪽 모두에 값을 세팅하는 것이 객체 상태 유지에 좋다!
```java
member.setTeam(team);
team.getMembers().add(member);
```
또는 편의 메서드 사용
```java
public void setTeam(Team team) {
    this.team = team;
    team.getMembers().add(this);
}
```

결론적으로
```java
//팀 저장
Team team=new Team();
team.setName("TeamA");
em.persist(team);

//회원 저장
Member member=new Member();
member.setUsername("member1");
member.setTeam(team); //연관관계의 주인에만 값을 넣어주기
em.persist(member);

team.getMembers().add(member); //둘 다 넣어주기

em.flush();
em.clear();

Team findTeam=em.find(Team.class, team.getId()); 
List<Member> members=findTeam.getMembers();

for(Member m : members){
        System.out.println("m = "+ m.getUsername());
}
```
양방향을 영속성 있게 세팅할 때는 양쪽에다가 값을 다 세팅해주는 게 맞다!



