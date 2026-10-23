# 地址详情页与持仓空数据调研

调研日期：2026-10-22，Asia/Shanghai。

## 结论与范围

建议保留轻量、可通过 URL 独立访问的地址详情页，优先打通真实地址查询，不继续扩展空数据界面。对于本项目“从持仓地址追踪流入、流出，再查看对手地址”的场景，独立页便于连续导航、保存研究上下文和复访；这是产品判断，不是由竞品数量直接证明的结论。

如果只需要在列表内快速查看最近几笔转账，抽屉也可以满足。当前已有路由和关注、备注，继续复用独立页比再做另一套承载方式更合理。持仓列表应是其中一个入口，而不是唯一入口。

本轮只做诊断和调研，未修改业务代码、启用数据源、创建账号、购买服务或改变生产配置。

## 本地实际接口证据

2026-10-22 直接读取当前开发服务的以下接口，均返回真实的业务状态，而非用浏览器测试夹具代替：

1. `GET http://127.0.0.1:5173/api/assets/ETH/holders`
   - `data: []`
   - `status: unsupported`
   - `reason: 此币种未接入Ethereum ERC-20持仓索引`
2. `GET http://127.0.0.1:5173/api/assets/LINK/holders`
   - `data: []`
   - `status: unavailable`
   - `reason: 旧版API迁移与生产使用条件未解锁；默认禁用`
3. `GET /api/addresses/{address}/transfers?asset=LINK`
   - 同样返回 `unavailable` 和旧版来源默认禁用的原因。

因此，ETH 页面没有地址行的直接原因是本项目没有接入对应原生币持仓能力；切到 ERC-20 或手工输入地址，也不能绕过当前转账来源禁用的问题。这不是链上没有地址或没有转账的证据。

代码定位：

- `src/main/java/io/observatory/Catalog.java:9`：ETH 的代币合约字段为 null。
- `src/main/java/io/observatory/ResearchService.java:159`：无 ERC-20 合约时持仓接口返回 unsupported。
- `src/main/java/io/observatory/ResearchService.java:161`：来源开关或审批开关未开启时返回 unavailable。
- `src/main/java/io/observatory/ResearchService.java:177`：转账接口也只覆盖有合约的 ERC-20。
- `src/main/java/io/observatory/ResearchService.java:179`：转账也受到相同来源开关限制。
- `frontend/src/main.tsx` 的 `AddressLookup` 已提供手工地址输入，但资产页对无合约币种回退到 LINK，可能导致 ETH 上下文与查询币种不一致。

上一轮自动化测试证明了导航、筛选、计算和布局行为，不代表真实持仓/转账来源已可用。应把这两种验收分别报告。

## 已核验的一手资料

### S1：Nansen Address Transactions

来源：[官方文档](https://docs.nansen.ai/api/profiler/address-transactions.md)。本轮成功读取。

官方接口直接接受地址和链，并提供日期、代币、对手方等过滤与分页；说明包含代币转移和原生币资金变动。文档同时明确覆盖与历史范围取决于链，不应从单页响应推导完整周期统计。

启示：地址交易检索可以成为独立能力，不必先得到某币种全部持有人名单。

### S2：Nansen Address Current Balances

来源：[官方文档](https://docs.nansen.ai/api/profiler/address-current-balances.md)。本轮成功读取。

官方提供按单地址和链查询当前资产、数量、USD 价值的能力，并明确这是某时点的当前余额快照，不是任意历史期间的余额变化。

启示：地址详情可以分阶段加入余额，但不应把当前余额直接当作周期流量。

### S3：Nansen Address Counterparties

来源：[官方文档](https://docs.nansen.ai/api/profiler/address-counterparties.md)。本轮成功读取。

官方提供单地址或实体的对手方及交互统计，包括数量、频率和时间等维度。

启示：要追查资金去向时，详情页里的 From/To 应可进入对应地址，且必须保留链与代币上下文。交易所身份等标签仍需要可追溯来源。

### S4：Nansen Address DEX Trades

来源：[官方文档](https://docs.nansen.ai/api/profiler/address-dex-trades.md)。本轮成功读取。

DEX 交易是单独的接口，提供买入与卖出的代币、数量、交易 USD 价值等字段。

启示：普通转账与交换不应共用未经验证的判定。首版可以先不提供交换筛选，待有明确的交易解析能力后再开放。

### S5：Alchemy alchemy_getAssetTransfers

来源：[官方文档](https://www.alchemy.com/docs/data/transfers-api/transfers-endpoints/alchemy-get-asset-transfers.md)。本轮成功读取。

接口按地址检索历史资产转移，支持来源地址、目标地址与分页参数；示例包含 ETH 转移。文档也区分内部转移的链覆盖范围。

启示：存在不依赖持有人排行榜的地址查询技术路线。接入候选接口前仍需用真实地址验证进出方向、精度、时间、翻页、去重和覆盖边界。本轮只核验文档，未使用业务密钥调用其数据接口，也未核验套餐成本或再分发许可，不能把候选能力写成已接通。

## 无法确认的项目

- Arkham：本轮通过独立新标签页访问官方站点和教程入口均遇到连接重置，未取得可核验正文；不将其作为本次必要性结论的证据。工作中曾提前提到其搜索能力，最终结论不依赖该未经本轮核验的描述。
- Etherscan：本轮文档读取未成功，故不声称已核验其当前接口套餐或持有人名单能力。
- Agent Reach 的 Exa 命令依赖 `mcporter` 在当前 shell 不可用；没有安装或修改环境，改用官方文档直接读取。
- 未证明任一新候选来源已经可以稳定支撑本项目的真实展示。

## 推荐页面和入口

1. 独立的“查地址”入口，显式选择链和资产；持仓列表为空时也可用。
2. 持仓地址链接、关注地址链接和转账对手地址链接都进入同一个详情路由。
3. 详情首版只保留地址、链、代币、转入/转出记录、时间、From、To、数量、交易哈希、来源状态与分页。
4. 保留关注和备注以支持复访；交换、标签、历史 USD 估值、完整周期统计按真实数据能力逐项开放。
5. 空状态分清“链/资产未支持”“来源故障或未启用”“筛选无结果”“成功查询但无事件”，展示对应的下一步。
6. 可增加区块浏览器外链作为临时查看途径，但它不能代替本产品数据接通，也不等于获得第三方数据使用许可。

## 实施优先级及验收

1. 修正 ETH 等原生币的能力说明与查询上下文，禁止静默跳到 LINK。验收：选 ETH 时始终明确看到 ETH 的能力状态。
2. 单独验证一个有公开活动的地址的真实转入/转出查询。验收：生产来源响应可复核到交易哈希，明确原生币和 ERC-20 两类能力，不将缺失数据写成零。
3. 接通独立地址入口与关注入口。验收：持仓列表为空时仍能进入所查地址详情；刷新和返回后上下文正确。
4. 再做持有人发现和排名。验收：明确当前页与全量排名的区别，不将部分数据标为 Top30。
5. 最后加入完整周期统计、交换和 USD 历史估值。验收：有完整窗口覆盖证据、专门的交换解析和对应事件时点的价格来源。
