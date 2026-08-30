package com.beercompetition.controller.admin;
import com.beercompetition.common.result.Result; import com.beercompetition.pojo.dto.*; import com.beercompetition.pojo.vo.*; import com.beercompetition.service.JudgeRecruitmentService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/judge-recruitments")
public class AdminJudgeRecruitmentController { private final JudgeRecruitmentService service;
 @GetMapping public Result<List<JudgeRecruitmentVO>> list(@RequestParam(required=false) String status,@RequestParam(required=false) String keyword){return Result.success(service.adminList(status,keyword));}
 @GetMapping("/{id}") public Result<JudgeRecruitmentVO> get(@PathVariable Long id){return Result.success(service.adminGet(id));}
 @PostMapping public Result<JudgeRecruitmentVO> create(@RequestBody @Valid JudgeRecruitmentRequest q){return Result.success(service.save(q,null));}
 @PutMapping("/{id}") public Result<JudgeRecruitmentVO> update(@PathVariable Long id,@RequestBody @Valid JudgeRecruitmentRequest q){return Result.success(service.save(q,id));}
 @PostMapping("/{id}/publish") public Result<JudgeRecruitmentVO> publish(@PathVariable Long id){return Result.success(service.publish(id));}
 @PostMapping("/{id}/close") public Result<JudgeRecruitmentVO> close(@PathVariable Long id){return Result.success(service.close(id));}
 @PostMapping("/{id}/reopen") public Result<JudgeRecruitmentVO> reopen(@PathVariable Long id){return Result.success(service.reopen(id));}
 @GetMapping("/{id}/applications") public Result<List<JudgeRecruitmentApplicationVO>> apps(@PathVariable Long id,@RequestParam(required=false) String status,@RequestParam(required=false) String keyword){return Result.success(service.applications(id,status,keyword));}
 @PatchMapping("/applications/{id}/status") public Result<JudgeRecruitmentApplicationVO> review(@PathVariable Long id,@RequestBody @Valid JudgeRecruitmentReviewRequest q){return Result.success(service.review(id,q));}
}
