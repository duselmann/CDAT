package gov.cida.cdat.control;

import akka.dispatch.OnComplete;

abstract public class Callback extends OnComplete<Message>{

	@Override
	abstract public void onComplete(Throwable t, Message repsonse) throws Throwable;

}
