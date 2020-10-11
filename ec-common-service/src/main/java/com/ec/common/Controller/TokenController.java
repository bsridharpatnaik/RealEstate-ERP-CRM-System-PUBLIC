package com.ec.common.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.AndroidTokenDetailsDTO;
import com.ec.common.Data.TokenData;
import com.ec.common.Service.AndroidTokenDetailsService;

@RestController
@RequestMapping(value = "/token", produces =
{ "application/json", "text/json" })
public class TokenController
{

	@Autowired
	AndroidTokenDetailsService atdService;

	@PutMapping
	@ResponseStatus(HttpStatus.OK)
	public void updateTokenForUser(@RequestBody TokenData td) throws Exception
	{
		atdService.updateTokenForUser(td.getToken());
	}

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<AndroidTokenDetailsDTO> getAllTokens()
	{
		return atdService.fetchAllTokens();
	}
}
