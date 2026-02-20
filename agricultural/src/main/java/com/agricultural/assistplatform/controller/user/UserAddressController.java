package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserAddressService;
import com.agricultural.assistplatform.vo.user.UserAddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户端-收货地址")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Operation(summary = "地址列表")
    @GetMapping("/addresses")
    public Result<List<UserAddressVO>> list() {
        return Result.ok(userAddressService.list());
    }

    @Operation(summary = "添加地址")
    @PostMapping("/addresses")
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        userAddressService.add(body);
        return Result.ok();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        userAddressService.setDefault(id);
        return Result.ok();
    }
}
