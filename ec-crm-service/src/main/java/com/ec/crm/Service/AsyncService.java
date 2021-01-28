package com.ec.crm.Service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService
{
	@Async("processExecutor")
	public void run(final Runnable runnable)
	{
		runnable.run();
	}
}
