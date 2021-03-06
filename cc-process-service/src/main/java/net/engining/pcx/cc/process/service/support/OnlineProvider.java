package net.engining.pcx.cc.process.service.support;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;

import net.engining.gm.facility.SystemStatusFacility;
import net.engining.gm.infrastructure.enums.SystemStatusType;
import net.engining.pcx.cc.infrastructure.shared.model.CactEndChangeAcct;
import net.engining.pcx.cc.infrastructure.shared.model.CactSubAcct;
import net.engining.pg.parameter.OrganizationContextHolder;

public class OnlineProvider implements Provider7x24
{
	@Autowired
	private SystemStatusFacility systemStatusFacility;

	@PersistenceContext
	private EntityManager em;
	
	@Override
	public LocalDate getCurrentDate()
	{
		return new LocalDate(systemStatusFacility.getSystemStatus().businessDate);
	}

	@Override
	public BigDecimal getBalance(CactSubAcct cactSubAcct)
	{
		//联机时使用当前余额
		return cactSubAcct.getCurrBal();
	}

	@Override
	public void increaseBalance(CactSubAcct cactSubAcct, BigDecimal balanceDelta)
	{
		SystemStatusType sysStatus = systemStatusFacility.getNowSystemStatus();
		//联机非批量时间入账
		if(sysStatus.equals(SystemStatusType.N))
		{
			cactSubAcct.setCurrBal(cactSubAcct.getCurrBal().add(balanceDelta));
			cactSubAcct.setEndDayBal(cactSubAcct.getEndDayBal().add(balanceDelta));
		}
		else if(sysStatus.equals(SystemStatusType.B)){
			//联机批量时间入账、批量入第二日账，走此逻辑	
			
			cactSubAcct.setCurrBal(cactSubAcct.getCurrBal().add(balanceDelta));
			
			CactEndChangeAcct changeAcct = new CactEndChangeAcct();
			
			changeAcct.setAcctSeq(cactSubAcct.getAcctSeq());
			changeAcct.setSubAcctId(cactSubAcct.getSubAcctId());
			changeAcct.setTxnDate(getCurrentDate().toDate());
			changeAcct.setOrg(OrganizationContextHolder.getCurrentOrganizationId());
			em.persist(changeAcct);
		}
	}

	@Override
	public boolean shouldDeferOffset()
	{
		SystemStatusType sysStatus = systemStatusFacility.getNowSystemStatus();
		
		//核心批量时需要做延迟冲销
		return sysStatus == SystemStatusType.B;
	}

	@Override
	public boolean allowRaiseAge()
	{
		// 联机交易时只能降低账龄
		return false;
	}

	@Override
	public boolean isInternalAccountAsBatch()
	{
		return false;
	}

	@Override
	public boolean shouldDeferPenaltySettle()
	{
		return shouldDeferOffset();
	}
}
