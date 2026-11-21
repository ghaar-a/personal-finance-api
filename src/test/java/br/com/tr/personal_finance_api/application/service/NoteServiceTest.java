
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Note;
import br.com.tr.personal_finance_api.domain.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteService noteService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void shouldPreserveOriginalContentInVulnerableMode() {
        ReflectionTestUtils.setField(noteService, "securityMode", "VULNERABLE");

        String content = "<script>alert('xss')</script><b>Texto</b>";

        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Note result = noteService.createNote(userId, content);

        assertEquals(content, result.getContent());
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getCreatedAt());

        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void shouldSanitizeDangerousContentInSecureMode() {
        ReflectionTestUtils.setField(noteService, "securityMode", "SECURE");

        String content =
                "<script>alert('xss')</script>"
                        + "<b>Texto seguro</b>"
                        + "<a href=\"javascript:alert(1)\">link</a>";

        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Note result = noteService.createNote(userId, content);

        assertFalse(result.getContent().contains("<script>"));
        assertFalse(result.getContent().contains("javascript:"));
        assertTrue(result.getContent().contains("<b>Texto seguro</b>"));
        assertTrue(result.getContent().contains("link"));
        assertEquals(userId, result.getUserId());

        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void shouldThrowExceptionWhenContentIsNull() {
        ReflectionTestUtils.setField(noteService, "securityMode", "SECURE");

        assertThrows(
                RuntimeException.class,
                () -> noteService.createNote(userId, null)
        );

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void shouldReturnNotesAssociatedWithUser() {
        List<Note> expectedNotes = List.of(
                Note.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .content("Primeira nota")
                        .build(),
                Note.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .content("Segunda nota")
                        .build()
        );

        when(noteRepository.findByUserId(userId))
                .thenReturn(expectedNotes);

        List<Note> result = noteService.getUserNotes(userId);

        assertEquals(expectedNotes, result);
        assertEquals(2, result.size());

        verify(noteRepository).findByUserId(userId);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotes() {
        when(noteRepository.findByUserId(userId))
                .thenReturn(List.of());

        List<Note> result = noteService.getUserNotes(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(noteRepository).findByUserId(userId);
    }
}
