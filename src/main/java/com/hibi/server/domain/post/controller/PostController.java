package com.hibi.server.domain.post.controller;

import com.hibi.server.domain.post.dto.request.PostCreateRequest;
import com.hibi.server.domain.post.dto.request.PostUpdateRequest;
import com.hibi.server.domain.post.dto.response.PostResponse;
import com.hibi.server.domain.post.service.PostService;
import com.hibi.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/daily-posts")
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(
            summary = "게시글 생성",
            description = "새로운 게시글을 생성합니다. 요청 본문에 제목, 소개글, 노래 ID 등의 정보를 포함해야 합니다."
    )
    public ResponseEntity<SuccessResponse<PostResponse>> create(@RequestBody PostCreateRequest request) {
        PostResponse response = postService.create(request);
        return ResponseEntity.ok(SuccessResponse.success("게시글 생성 성공", response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "게시글 수정",
            description = "ID를 통해 특정 게시글의 내용을 수정합니다."
    )
    public ResponseEntity<SuccessResponse<PostResponse>> update(
            @PathVariable Long id,
            @RequestBody PostUpdateRequest request
    ) {
        PostResponse response = postService.update(id, request);
        return ResponseEntity.ok(SuccessResponse.success("게시글 수정 성공", response));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "게시글 삭제",
            description = "ID를 통해 특정 게시글을 삭제합니다."
    )
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.ok(SuccessResponse.success("게시글 삭제 성공", null));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "ID로 게시글 조회",
            description = "게시글 ID를 통해 단일 게시글 정보를 조회합니다."
    )
    public ResponseEntity<SuccessResponse<PostResponse>> getById(@PathVariable Long id) {
        PostResponse response = postService.getById(id);
        return ResponseEntity.ok(SuccessResponse.success("게시글 조회 성공", response));
    }

    @GetMapping("/date")
    @Operation(
            summary = "특정 날짜 게시글 조회",
            description = "postedAt 값이 지정된 날짜와 일치하는 게시글을 조회합니다."
    )
    public ResponseEntity<SuccessResponse<PostResponse>> getByDate(@RequestParam("date") LocalDate date) {
        PostResponse response = postService.getByPostedAt(date);
        return ResponseEntity.ok(SuccessResponse.success("특정 날짜 게시글 조회 성공", response));
    }

    @GetMapping("/range")
    @Operation(
            summary = "기간 내 게시글 조회",
            description = "시작일과 종료일 사이에 작성된 게시글 리스트를 조회합니다."
    )
    public ResponseEntity<SuccessResponse<List<PostResponse>>> getByRange(
            @RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end
    ) {
        List<PostResponse> responseList = postService.getByPostedAtBetween(start, end);
        return ResponseEntity.ok(SuccessResponse.success("기간별 게시글 조회 성공", responseList));
    }

    @GetMapping
    @Operation(
            summary = "전체 게시글 조회",
            description = "등록된 모든 게시글을 조회합니다."
    )
    public ResponseEntity<SuccessResponse<List<PostResponse>>> getAll() {
        List<PostResponse> responseList = postService.getAll();
        return ResponseEntity.ok(SuccessResponse.success("전체 게시글 조회 성공", responseList));
    }
}
