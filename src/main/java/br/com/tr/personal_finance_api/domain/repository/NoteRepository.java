package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findByUserId(UUID userId);
}