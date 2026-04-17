package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Note;
import br.com.tr.personal_finance_api.domain.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Value("${app.security-mode:VULNERABLE}")
    private String securityMode;

    private static final PolicyFactory POLICY =
            Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    public Note createNote(UUID userId, String content) {

        if (content == null) {
            throw new RuntimeException("Conteúdo não pode ser nulo");
        }

        String finalContent;

        if ("SECURE".equalsIgnoreCase(securityMode)) {
            finalContent = POLICY.sanitize(content);
        } else {
            finalContent = content;
        }

        Note note = Note.builder()
                .userId(userId)
                .content(finalContent)
                .createdAt(OffsetDateTime.now())
                .build();

        return noteRepository.save(note);
    }

    public List<Note> getUserNotes(UUID userId) {
        return noteRepository.findByUserId(userId);
    }
}