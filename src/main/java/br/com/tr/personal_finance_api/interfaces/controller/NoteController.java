package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.NoteService;
import br.com.tr.personal_finance_api.domain.entity.Note;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public Note createNote(
            @RequestBody String content,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return noteService.createNote(user.getUserId(), content);
    }

    @GetMapping
    public List<Note> getMyNotes(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return noteService.getUserNotes(user.getUserId());
    }
}