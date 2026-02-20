package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserInfoService;
import com.agricultural.assistplatform.vo.user.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户端-个人中心")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final UserInfoService userInfoService;

    @Operation(summary = "用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(userInfoService.getInfo());
    }

    @Operation(summary = "修改信息")
    @PutMapping("/info")
    public Result<Void> update(@RequestBody Map<String, String> body) {
        userInfoService.updateInfo(body);
        return Result.ok();
    }
}
