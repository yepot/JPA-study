# chapter02. JPA 시작하기
## 1. 프로젝트 생성
가벼운 실습용 H2 데이터베이스 사용

Maven

JDK 17

H2 버전, JDK 버전에 맞게 수정한 pom.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>jpa-basic</groupId>
    <artifactId>jpa-basic</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependencies>
    <!-- JPA 하이버네이트 -->
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-entitymanager</artifactId>
        <version>5.3.10.Final</version>
    </dependency>

    <!-- H2 데이터베이스 -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>2.4.240</version>
    </dependency>

    <!-- JAXB API -->
    <dependency>
        <groupId>javax.xml.bind</groupId>
        <artifactId>jaxb-api</artifactId>
        <version>2.3.1</version>
    </dependency>

    <!-- JAXB Runtime -->
    <dependency>
        <groupId>org.glassfish.jaxb</groupId>
        <artifactId>jaxb-runtime</artifactId>
        <version>2.3.1</version>
    </dependency>

    <!-- Activation -->
    <dependency>
        <groupId>javax.activation</groupId>
        <artifactId>javax.activation-api</artifactId>
        <version>1.2.0</version>
    </dependency>
</dependencies>
    </dependencies>

</project>
```

JPA 설정 파일 - persistence.xml 생성
```
<?xml version="1.0" encoding="UTF-8"?>
<persistence version="2.2"
             xmlns="http://xmlns.jcp.org/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd">
    <persistence-unit name="hello">
        <properties>
            <!-- 필수 속성 -->
            <property name="javax.persistence.jdbc.driver" value="org.h2.Driver"/>
            <property name="javax.persistence.jdbc.user" value="sa"/>
            <property name="javax.persistence.jdbc.password" value=""/>
            <property name="javax.persistence.jdbc.url" value="jdbc:h2:tcp://localhost/~/test"/>
            <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
            <!-- 옵션 -->
            <property name="hibernate.show_sql" value="true"/>
            <property name="hibernate.format_sql" value="true"/>
            <property name="hibernate.use_sql_comments" value="true"/>
            <property name="hibernate.hbm2ddl.auto" value="create" />
        </properties>
    </persistence-unit>
</persistence>
```

#### 데이터베이스 방언
- JPA는 특정 데이터베이스에 종속 X
- 그래서 각각의 데이터베이스에서 표준적이지 않은 것들을 방언이라고 함

## 2. 애플리케이션 개발
### 2-1. JPA 구동 방식
```java
EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

EntityManager em = emf.createEntityManager(); //데이터베이스 커넥션을 하나 받음

EntityTransaction tx = em.getTransaction(); //트랜잭션을 얻을 수 있음
tx.begin(); //데이터베이스 트랜잭션 시작

try {
    Member member = new Member();
    member.setId(1L);
    member.setName("yepot");

    em.persist(member);

    tx.commit(); //저장
} catch (Exception e) {
    tx.rollback(); //문제 시 롤백
} finally {
    em.close(); //사용 후 닫아야함
}

emf.close(); //전체 애플리케이션 끝나면 엔티티팩토리도 닫아줌
```
#### 엔티티 매니저 팩토리 (EntityManagerFactory)
- 하나만 생성해서 애플리케이션 전체에서 공유
- 웹 서버가 올라오는 시점에 딱 하나만 생성됨

#### 엔티티 매니저 (EntityManager)
- 트랜잭션 단위마다 생성해줌
- 트랜잭션을 try-catch문으로 관리해줘야 함
- 쓰레드간에 절대 공유X (사용 후 버려야 함)!!

JPA의 모든 데이터 변경은 트랜잭션 안에서 실행

### 2.2 JPQL 소개
> JPQL (Java Persistence Query Language)

- JPA를 사용할 때, 엔티티 객체를 대상으로 검색하는 쿼리 언어
- SQL: 테이블을 대상으로 쿼리 / JPQL: 엔티티 객체를 대상으로 쿼리
- 즉, 객체지향 SQL 이라고 할 수 있다!


예) 간단한 회원 조회

```java
EntityManager.find(Member.class, 1L);
```
예) 회원 전체 조회하고 싶을 때 
```java
List<Member> result = em.createQuery("select m from Member as m", Member.class).getResultList();
```


