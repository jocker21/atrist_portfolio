package com.example.artist.controller;

import jakarta.validation.Valid;
import com.example.artist.entity.ContactMessage;
import com.example.artist.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactMessageRepository contactMessageRepository;

    // GET /contact — показать форму
    @GetMapping
    public String contactForm(Model model) {
        // Передаём пустой объект формы в шаблон
        // Thymeleaf привяжет поля формы к полям этого объекта
        model.addAttribute("contactForm", new ContactForm());
        return "contact";
    }

    // POST /contact — обработать отправленную форму
    @PostMapping
    public String submitContact(
            // @Valid — запускает валидацию полей формы
            // @ModelAttribute — Spring заполняет объект данными из формы
            @Valid @ModelAttribute("contactForm") ContactForm form,
            // BindingResult — содержит ошибки валидации (если есть)
            // ВАЖНО: BindingResult должен идти сразу после @Valid-параметра
            BindingResult bindingResult,
            Model model) {

        // Если есть ошибки валидации — вернуть форму с сообщениями об ошибках
        if (bindingResult.hasErrors()) {
            return "contact";
        }

        ContactMessage message = new ContactMessage();
        message.setName(form.getName());
        message.setEmail(form.getEmail());
        message.setMessage(form.getMessage());
        contactMessageRepository.saveAndFlush(message);

        // Передаём флаг успеха в шаблон для показа сообщения
        model.addAttribute("success", true);
        return "contact";
    }

    // Вложенный класс — DTO (Data Transfer Object) для формы контактов.
    // Хранит данные формы и правила валидации.
    @Data // Lombok: генерирует getters, setters, toString
    public static class ContactForm {

        @NotBlank(message = "{validation.name}")
        private String name;

        @NotBlank(message = "{validation.email}")
        @Email(message = "{validation.emailInvalid}")
        private String email;

        @NotBlank(message = "{validation.message}")
        private String message;
    }
}
