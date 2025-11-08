import { CompanyForm } from '../components/CompanyForm';
import { MatchPanel } from '../components/MatchPanel';

const App = () => {
  return (
    <div className="min-h-screen bg-slate-100">
      <header className="bg-indigo-700 py-8 text-center text-white">
        <h1 className="text-2xl font-bold">企业信息采集与智能匹配平台</h1>
        <p className="mt-2 text-sm text-indigo-100">通过 Dify 工作流完成企业入库，并使用 ChatFlow 精准匹配业务需求。</p>
      </header>
      <main className="mx-auto grid max-w-6xl gap-8 px-6 py-10 lg:grid-cols-[2fr_1fr]">
        <CompanyForm />
        <MatchPanel />
      </main>
    </div>
  );
};

export default App;
