/*
 * package com.ec.crm.Controller;
 * 
 * import java.util.HashMap; import java.util.List; import java.util.Map;
 * 
 * import javax.validation.Valid;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.data.domain.Page; import
 * org.springframework.data.domain.Pageable; import
 * org.springframework.data.domain.Sort.Direction; import
 * org.springframework.data.web.PageableDefault; import
 * org.springframework.http.HttpStatus; import
 * org.springframework.http.ResponseEntity; import
 * org.springframework.validation.FieldError; import
 * org.springframework.web.bind.MethodArgumentNotValidException; import
 * org.springframework.web.bind.annotation.DeleteMapping; import
 * org.springframework.web.bind.annotation.ExceptionHandler; import
 * org.springframework.web.bind.annotation.GetMapping; import
 * org.springframework.web.bind.annotation.PathVariable; import
 * org.springframework.web.bind.annotation.PostMapping; import
 * org.springframework.web.bind.annotation.PutMapping; import
 * org.springframework.web.bind.annotation.RequestBody; import
 * org.springframework.web.bind.annotation.RequestMapping; import
 * org.springframework.web.bind.annotation.ResponseStatus; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * import com.ec.crm.Data.SentimentListWithTypeAheadData; import
 * com.ec.crm.Data.SourceListWithTypeAheadData; import
 * com.ec.crm.Filters.FilterDataList; import com.ec.crm.Model.Sentiment; import
 * com.ec.crm.ReusableClasses.IdNameProjections; import
 * com.ec.crm.Service.SentimentService;
 * 
 * @RestController
 * 
 * @RequestMapping(value="/sentiment",produces = { "application/json",
 * "text/json" }) public class SentimentController {
 * 
 * @Autowired SentimentService sentimentService;
 * 
 * @GetMapping public Page<Sentiment> returnAllSentiment(Pageable pageable) {
 * return sentimentService.fetchAll(pageable); }
 * 
 * @PostMapping
 * 
 * @ResponseStatus(HttpStatus.OK) public SentimentListWithTypeAheadData
 * returnFilteredSource(@RequestBody FilterDataList
 * sentimentFilterDataList,@PageableDefault(page = 0, size = 10, sort =
 * "sentimentId", direction = Direction.DESC) Pageable pageable) { return
 * sentimentService.findFilteredSource(sentimentFilterDataList,pageable); }
 * 
 * @GetMapping("/{id}") public Sentiment findSentimentByID(@PathVariable long
 * id) throws Exception { return sentimentService.findSingleSentiment(id); }
 * 
 * @PostMapping("/create")
 * 
 * @ResponseStatus(HttpStatus.CREATED) public Sentiment
 * createSentiment(@Valid @RequestBody Sentiment sentiment) throws Exception{
 * 
 * return sentimentService.createSentiment(sentiment); }
 * 
 * @PutMapping("/{id}") public Sentiment updateSentiment(@PathVariable Long
 * id,@Valid @RequestBody Sentiment sentiment) throws Exception { return
 * sentimentService.updateSentiment(id, sentiment); }
 * 
 * @DeleteMapping("/{id}") public ResponseEntity<?>
 * deleteSentiment(@PathVariable Long id) throws Exception {
 * sentimentService.deleteSentiment(id); return
 * ResponseEntity.ok("Sentiment Deleted sucessfully."); }
 * 
 * @GetMapping("/idandnames") public List<IdNameProjections> returnIdAndNames()
 * { return sentimentService.findIdAndNames(); }
 * 
 * @ResponseStatus(HttpStatus.BAD_REQUEST)
 * 
 * @ExceptionHandler(MethodArgumentNotValidException.class) public Map<String,
 * String> handleValidationExceptions( MethodArgumentNotValidException ex) {
 * Map<String, String> errors = new HashMap<>();
 * ex.getBindingResult().getAllErrors().forEach((error) -> { String fieldName =
 * ((FieldError) error).getField(); String errorMessage =
 * error.getDefaultMessage(); errors.put(fieldName, errorMessage); }); return
 * errors; } }
 */