// 旧结算页会把任意一场对局写成无尽模式。正式结算已经在对局页完成并上传。
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { PageBody, PageHeader } from '@/components/common/PageHeader';

export default function ResultPage() {
  return (
    <PageBody className="max-w-3xl">
      <PageHeader
        eyebrow="结算 · SETTLEMENT"
        title="这一局的记录已经存好"
        description="对局结束时就写入了本轮成绩，这里不再重复上传——避免把限时或词源对局记成无尽模式。"
      />

      <div className="mt-8 flex flex-wrap gap-3">
        <Button asChild>
          <Link to="/prepare">再来一局 →</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/profile">查看个人数据</Link>
        </Button>
      </div>
    </PageBody>
  );
}
