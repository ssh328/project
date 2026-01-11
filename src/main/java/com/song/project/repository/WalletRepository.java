package com.song.project.repository;

import com.song.project.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // 사용자 ID로 지갑 조회
    Optional<Wallet> findByUser_Id(Long userId);
}
