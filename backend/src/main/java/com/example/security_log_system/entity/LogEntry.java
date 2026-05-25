package com.example.security_log_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Nginx 로그 핵심 정보
@Entity                             // 이 클래스가 JPA 엔티티임을 선언, 즉 이 클래스는 DB테이블과 1:1로 매핑되는 객체
@Table(name = "network_logs")       // 이 엔티티가 DB의 어느 테이블에 저장될지 지정
@Getter                             // 모든 필드에 대한 Getter메서드 자동 생성
@NoArgsConstructor                  // 파라미터가 없는 기본 생성자를 생성
@AllArgsConstructor                 // 모든 필드를 파라미터로 받는 생성자를 생성
@Builder                            // 빌더 패턴 사용할 수 있게 설정. (new LogEntry().. 같은 생성자 형태 대신 사용)
public class LogEntry {

    @Id             // @Id: 해당 필드를 테이블의 Primary Key(PK,기본키)로 지정.
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // PK값을 자동으로 생성하는 방식, IDENTITY는 DB가 알아서 ID값을 1씩 증가시키도록 설정.
    private Long id;

    private String ipAddress;       // 접속한 IP
    private String requestMethod;   // GET, POST 등
    private String requestUrl;      // 접속 경로 (/admin 등)
    private int statusCode;         // 200,403 등

    // @Column(columnDefinition = "TEXT"):rawLog처럼 내용이 길어질 수 있는 데이터는 기본 문자열 타입보다 큰 TEXT타입 사용하도록 명시
    @Column(columnDefinition = "TEXT")
    private String rawLog;          // 전체 원본 로그

    private LocalDateTime createdAt; // 로그 수신 시간


    /*public enum AnalysisState {
        PENDING,
        COMPLETED,
        FAILED
    }*/
}

/*
LogEntry 저장 직후
→ analysisStatus = PENDING

AI 결과 수신 후
→ analysisStatus = COMPLETED
→ DetectedThreat 저장

AI 처리 실패 또는 타임아웃
→ analysisStatus = FAILED*/
