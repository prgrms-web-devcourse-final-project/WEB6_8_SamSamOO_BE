package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.service.PostDummyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;

@WebMvcTest(
    controllers = PostDummyController.class,
    excludeAutoConfiguration = {
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JpaBaseConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class PostDummyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostDummyService dummyService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("더미 멤버 생성 API 테스트")
    void testCreateDummyMembers() throws Exception {
        Mockito.when(dummyService.createDummyMembers(anyInt())).thenReturn(5);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/dummy/members")
                .param("count", "5")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("더미 멤버 5명 생성 완료"));
    }

    @Test
    @DisplayName("더미 멤버 투표 API 테스트")
    void testDummyVote() throws Exception {
        Mockito.when(dummyService.dummyVote(anyLong())).thenReturn(3);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/dummy/vote")
                .param("postId", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("더미 멤버 3명 투표 완료"));
    }

    @Test
    @DisplayName("더미 멤버 삭제 API 테스트")
    void testDeleteDummyMembers() throws Exception {
        Mockito.when(dummyService.deleteDummyMembers()).thenReturn(2);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/dummy/members"))
                .andExpect(status().isOk())
                .andExpect(content().string("더미 멤버 2명 삭제 완료"));
    }
}
