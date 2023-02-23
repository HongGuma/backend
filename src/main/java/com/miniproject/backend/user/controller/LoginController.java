package com.miniproject.backend.user.controller;

import com.miniproject.backend.global.dto.ErrorDTO;
import com.miniproject.backend.global.dto.ResponseDTO;
import com.miniproject.backend.global.jwt.auth.AuthToken;
import com.miniproject.backend.global.jwt.auth.AuthTokenProvider;
import com.miniproject.backend.user.domain.User;
import com.miniproject.backend.user.dto.LoginRequestDTO;
import com.miniproject.backend.user.dto.LoginResponseDTO;
import com.miniproject.backend.user.dto.UserRequestDTO;
import com.miniproject.backend.user.exception.UserException;
import com.miniproject.backend.user.exception.UserExceptionType;
import com.miniproject.backend.user.service.LoginService;
import com.miniproject.backend.user.service.TokenService;

import com.miniproject.backend.user.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;
    private final AuthTokenProvider authTokenProvider;
    private final TokenService tokenService;
    private final UserServiceImpl userService;

    @PostMapping("/login")
    public ResponseDTO<?> login(HttpServletResponse response, @RequestBody LoginRequestDTO requestDTO) {

        User user = loginService.login(requestDTO);

        AuthToken authToken = getToken(response, user);

        LoginResponseDTO loginResponseDTO
                = LoginResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .brith(user.getBirth())
                .joinType(user.getJoinType())
                .accessToken(authToken.getToken())
                .build();

        return new ResponseDTO<>().ok(loginResponseDTO, "로그인 성공");
    }


    @PostMapping("/refresh")
    public ResponseDTO<?> reissue(HttpServletResponse response, HttpServletRequest request){
        String token = resetRefreshToken(request);
        User user = tokenService.checkValid(token);

        AuthToken authToken = getToken(response, user);

        return new ResponseDTO<>().ok(authToken.getToken(), "refresh 성공");
    }

    public AuthToken getToken(HttpServletResponse response, User user){
        AuthToken authToken = authTokenProvider.issueAccessToken(user);
        AuthToken refreshToken = authTokenProvider.issueRefreshToken(user);

        Cookie cookie = new Cookie("refresh", refreshToken.getToken());
        tokenService.createRefreshToken(user,refreshToken.getToken());

        response.addCookie(cookie);

        log.info(String.valueOf(authToken.getToken()));

        return authToken;
    }

    public String resetRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = "";
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                token = cookie.getValue();
                cookie = new Cookie("refresh", null);
                cookie.setMaxAge(0);

            }
        }
        return token;
    }

    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "회원가입 api")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 생성 성공"),
            @ApiResponse(responseCode = "-101", description = "이메일 중복",
                    content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "-102", description = "이메일 형식",
                    content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping("/signUp")
    public ResponseDTO<?> signup(@RequestBody UserRequestDTO userRequestDTO){
        if(userRequestDTO.getEmail().matches("^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")){
            userService.signup(userRequestDTO);
            return new ResponseDTO<>().ok(true,"회원 생성 성공");
        }else{
            throw new UserException(UserExceptionType.NOT_EMAIL_FORMAT);
        }

    }

    @Operation(summary = "이메일 중복 확인 API", description = "true = 중복아님, false = 중복")
    @PostMapping("/signUp/email")
    public ResponseDTO<?> checkEmail(@RequestParam("email") String email){
        if(userService.checkDuplicationEmail(email)){
            return new ResponseDTO<>().ok(true,"사용가능한 이메일 입니다.");
        }else
            return new ResponseDTO<>(401,false,false,"중복된 이메일 입니다.");
    }

    @Operation(summary = "로그아웃 API")
    @RequestMapping(value = "/user/logout", method = RequestMethod.POST)
    public ResponseDTO<?> logout(HttpServletRequest request) {
        String token = resetRefreshToken(request);
        tokenService.delete(token);
        return new ResponseDTO<>().ok(null, "로그아웃 완료");
    }
}
