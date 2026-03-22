package hellojpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager(); //데이터베이스 커넥션을 하나 받음

        EntityTransaction tx = em.getTransaction(); //트랜잭션을 얻을 수 있음
        tx.begin();

        try {

            // 비영속
            Member member = new Member();
            member.setId(101L);
            member.setName("HelloJPA");

            // 영속
            em.persist(member);

            // em.flush(); //미리 데이터베이스에 반영 또는 미리 쿼리 보고싶을 때 플러시 강제 호출

            Member findMember1 = em.find(Member.class, 101L); //쿼리 안나감
            Member findMember2 = em.find(Member.class, 101L); //쿼리 안나감

            System.out.println("result = " + (findMember1 == findMember2)); //true(동일성 보장)

            tx.commit(); // 커밋하는 순간 데이터베이스에 쿼리 보냄
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }
}