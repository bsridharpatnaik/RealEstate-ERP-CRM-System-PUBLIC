package com.ec.common.Controller;

import com.ec.common.Data.ProjectConstantsUpdateDTO;
import com.ec.common.Model.ProjectConstants;
import com.ec.common.Service.ProjectConstantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/project-constants")
public class ProjectConstantsController {

    @Autowired
    private ProjectConstantsService projectConstantsService;

    // Get all project constants
    @GetMapping
    public ResponseEntity<List<ProjectConstants>> getAllProjectConstants() {
        List<ProjectConstants> projectConstants = projectConstantsService.getAllProjectConstants();
        return ResponseEntity.ok(projectConstants);
    }

    // Get a single project constant by id
    @GetMapping("/{id}")
    public ResponseEntity<ProjectConstants> getSingleProjectConstant(@PathVariable Long id) {
        try {
            ProjectConstants projectConstant = projectConstantsService.getSingleProjectConstants(id);
            return ResponseEntity.ok(projectConstant);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Update a project constant by id
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProjectConstant(
            @PathVariable Long id,
            @Valid @RequestBody ProjectConstantsUpdateDTO updateDTO,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        try {
            ProjectConstants updatedProjectConstant = projectConstantsService.updateConstants(id, updateDTO.getValue());
            return ResponseEntity.ok(updatedProjectConstant);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
